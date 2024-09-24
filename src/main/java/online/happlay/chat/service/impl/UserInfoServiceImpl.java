package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserQueryDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.enums.*;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.po.UserInfoBeauty;
import online.happlay.chat.entity.vo.user.UserInfoVO;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.user.UserStatusEnum;
import online.happlay.chat.enums.userBeauty.BeautyAccountStatusEnum;
import online.happlay.chat.enums.userContact.JoinTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserInfoMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IChatSessionUserService;
import online.happlay.chat.service.IUserContactService;
import online.happlay.chat.service.IUserInfoBeautyService;
import online.happlay.chat.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.utils.StringTools;
import online.happlay.chat.websocket.netty.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    @Lazy
    private IUserContactService userContactService;

    private final CommonConfig commonConfig;

    private final RedisComponent redisComponent;

    @Lazy
    @Resource
    private MessageHandler messageHandler;

    @Lazy
    @Resource
    private IChatSessionUserService chatSessionUserService;

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

        // 初始化机器人好友
        userContactService.addContactForRobot(userId);
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

        // 查询联系人
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, userInfo.getUserId())
                .eq(UserContact::getStatus, UserContactStatusEnum.FRIEND.getStatus());
        List<UserContact> userContacts = userContactService.list(queryWrapper);

        // 获取联系人ID
        List<String> contactIdList = userContacts.stream()
                .map(UserContact::getContactId).collect(Collectors.toList());


        // 存入redis缓存
        redisComponent.cleanUserContact(userInfo.getUserId());
        if (!contactIdList.isEmpty()) {
            redisComponent.addUserContactBatch(userInfo.getUserId(), contactIdList);
        }

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
    public void updatePassword(UserTokenDTO userToken, String password) {
        UserInfo userInfo = this.getById(userToken.getUserId());
        // 加密存入数据库
        userInfo.setPassword(StringTools.encodeMd5(password));
        this.updateById(userInfo);
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {
        UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
        if (userStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getUserId, userId);
        UserInfo userInfo = this.getById(queryWrapper);
        userInfo.setStatus(userStatusEnum.getStatus());
        this.updateById(userInfo);
    }

    @Override
    public void forceOffLine(String userId) {
        // 强制下线
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setContactType(UserContactTypeEnum.USER.getType());
        messageSendDTO.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
        messageSendDTO.setContactId(userId);
        messageHandler.sendMessage(messageSendDTO);
    }

    @Override
    public PaginationResultVO<UserInfo> loadUser(UserQueryDTO userQueryDTO) {
        // 获取当前页码和每页大小
        Integer pageNo = userQueryDTO.getPageNo();
        Integer pageSize = userQueryDTO.getPageSize();

        // 创建分页对象
        Page<UserInfo> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 判断并添加查询条件
        if (userQueryDTO.getUserId() != null && !userQueryDTO.getUserId().isEmpty()) {
            queryWrapper.like(UserInfo::getUserId, userQueryDTO.getUserId());
        }

        if (userQueryDTO.getNickName() != null && !userQueryDTO.getNickName().isEmpty()) {
            queryWrapper.like(UserInfo::getNickName, userQueryDTO.getNickName());
        }

        // 计算总记录数
        int total = (int) this.count(queryWrapper);

        // 计算总页数 (总记录数 / 每页记录数，向上取整)
        int pages = (int) Math.ceil((double) total / pageSize);

        // 添加排序条件
        queryWrapper.orderByDesc(UserInfo::getCreateTime);

        Page<UserInfo> newPage = this.page(page, queryWrapper);

        System.out.println(newPage.getTotal());
        return new PaginationResultVO<UserInfo>(
                total,
                pageSize,
                pageNo,
                pages,
                newPage.getRecords()
        );


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
        if (!dbInfo.getNickName().equals(userInfo.getNickName())) {
            contactNameUpdate = userInfo.getNickName();
        }

        if (contactNameUpdate == null) {
            return;
        }

        // 更新redis中token中的昵称
        UserTokenDTO userTokenDTO = redisComponent.getUserTokenDTOByUserId(userInfo.getUserId());
        userTokenDTO.setNickName(contactNameUpdate);
        redisComponent.saveUserTokenDTO(userTokenDTO);

        // 更新会话信息表中的昵称信息
        chatSessionUserService.updateNameByContactId(userInfo.getUserId(), userInfo.getNickName());

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
