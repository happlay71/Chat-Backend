package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.enums.BeautyAccountStatusEnum;
import online.happlay.chat.enums.JoinTypeEnum;
import online.happlay.chat.enums.UserContactTypeEnum;
import online.happlay.chat.enums.UserStatusEnum;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.po.UserInfoBeauty;
import online.happlay.chat.entity.vo.UserInfoVO;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserInfoMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IUserInfoBeautyService;
import online.happlay.chat.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static online.happlay.chat.constants.Constants.LENGTH_20;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    private final IUserInfoBeautyService userInfoBeautyService;

    private final CommonConfig commonConfig;

    private final RedisComponent redisComponent;

    @Override
    public UserInfo getByEmail(String email) {
        // 使用 LambdaQueryWrapper 来构建查询条件
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);

        // 查询是否存在该邮箱的用户
        return this.getOne(queryWrapper);
    }

    @Override
    public void register(String email, String nickName, String password) {
        UserInfo userInfo = getByEmail(email);

        if (userInfo != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        String userId = StringTools.getUserId();

        // 设置靓号---需先存在靓号信息，再去匹配用户信息 TODO 待修改为用户申请，然后管理员发配
        UserInfoBeauty beauty = userInfoBeautyService.getByEmail(email);
        Boolean useBeauty = null != beauty && BeautyAccountStatusEnum.NO_USE.getStatus().equals(beauty.getStatus());

        if (useBeauty) {
            userId = UserContactTypeEnum.USER.getPrefix() + beauty.getUserId();
        }

        // 存入数据库
        LocalDateTime time = LocalDateTime.now();

        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
        userInfo.setPassword(StringTools.encodeMd5(password));
        userInfo.setCreateTime(time);
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setLastOffTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        this.save(userInfo);

        // 修改靓号状态为 已使用
        if (useBeauty) {
            userInfoBeautyService.useBeauty(beauty);
        }

        // TODO 初始化机器人好友
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO login(String email, String password) {
        UserInfo userInfo = this.getByEmail(email);

        // 判断账户状态
        if (null == userInfo || !userInfo.getPassword().equals(StringTools.encodeMd5(password))) {
            throw new BusinessException("账户或密码不正确");
        }

        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }

        // TODO 查询我的群组
        // TODO 查询我的联系人
        UserTokenDTO userTokenDTO = getUserTokenDTO(userInfo);

        // 获取心跳
        Long lastHeartBeat = redisComponent.getUserHeartBeat(userInfo.getUserId());
        if (lastHeartBeat != null) {
            throw new BusinessException("此账号已登录，请退出后再登录");
        }

        // 保存登录信息到redis
        String tokenStr = userTokenDTO.getUserId() + StringTools.getRandomString(LENGTH_20);
        String token = StringTools.encodeMd5(tokenStr);
        userTokenDTO.setToken(token);
        redisComponent.saveUserTokenDTO(userTokenDTO);

        // 转换为UserInfoVO
        return changeToUserInfoVO(userInfo, userTokenDTO);

    }

    @Override
    public UserInfoVO getUserInfo(UserTokenDTO userToken) {
        UserInfo userInfo = this.getById(userToken.getUserId());
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        userInfoVO.setAdmin(userToken.getAdmin());
        return userInfoVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
        if (avatarFile != null) {
            String baseFolder = commonConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
            if (!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }

            String filePath = targetFileFolder.getPath() + "/" + userInfo.getUserId() + Constants.IMAGE_SUFFIX;
            avatarFile.transferTo(new File(filePath));
            avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
        }
        // 先查询后提交，防止提交时事务开启导致查询失败
        UserInfo dbInfo = this.getById(userInfo.getUserId());
        this.updateById(userInfo);

        String contactNameUpdate = null;
        if (dbInfo.getNickName().equals(userInfo.getNickName())) {
            contactNameUpdate = userInfo.getNickName();
        }

        // TODO 更新会话信息中的昵称信息
    }

    private static UserInfoVO changeToUserInfoVO(UserInfo userInfo, UserTokenDTO userTokenDTO) {
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        userInfoVO.setToken(userTokenDTO.getToken());
        userInfoVO.setAdmin(userTokenDTO.getAdmin());
        return userInfoVO;
    }

    private UserTokenDTO getUserTokenDTO(UserInfo userInfo) {
        UserTokenDTO userTokenDTO = new UserTokenDTO();
        userTokenDTO.setUserId(userInfo.getUserId());
        userTokenDTO.setNickName(userInfo.getNickName());

        String adminEmails = commonConfig.getAdminEmails();
        // 判断是否为管理员
        userTokenDTO.setAdmin(
                StrUtil.isNotBlank(adminEmails)
                && ArrayUtils.contains(adminEmails.split(","), userInfo.getEmail())
        );
        return userTokenDTO;
    }
}
