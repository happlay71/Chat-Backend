package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.GroupDetails;
import online.happlay.chat.entity.vo.GroupInfoVO;
import online.happlay.chat.entity.vo.MyGroups;
import online.happlay.chat.entity.vo.UserContactVO;
import online.happlay.chat.enums.GroupStatusEnum;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.enums.UserContactStatusEnum;
import online.happlay.chat.enums.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.GroupInfoMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IGroupInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserContactService;
import online.happlay.chat.service.IUserInfoService;
import online.happlay.chat.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 群 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Service
@RequiredArgsConstructor
public class GroupInfoServiceImpl extends ServiceImpl<GroupInfoMapper, GroupInfo> implements IGroupInfoService {

    private final RedisComponent redisComponent;

    private final CommonConfig commonConfig;

    private final IUserContactService userContactService;

    private final IUserInfoService userInfoService;

    // 新增
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
        LambdaQueryWrapper<GroupInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupInfo::getGroupOwnerId, groupInfo.getGroupOwnerId());
        long count = this.count(queryWrapper);
        // 个人拥有群组数超过限制抛出异常
        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        if (count >= sysSettingDTO.getMaxGroupCount()) {
            throw new BusinessException("超出个人创建群组的最大限制！");
        }

        if (null == avatarFile) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        groupInfo.setCreateTime(LocalDateTime.now());
        groupInfo.setGroupId(StringTools.getGroupId());
        this.save(groupInfo);

        // 将群组添加到联系人数据库
        UserContact userContact = new UserContact();
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContact.setContactType(UserContactTypeEnum.GROUP.getType());
        userContact.setContactId(groupInfo.getGroupId());
        userContact.setUserId(groupInfo.getGroupOwnerId());
        userContact.setCreateTime(LocalDateTime.now());
        userContactService.save(userContact);

        // TODO 创建会话

        // TODO 发送初始消息

        // 保存群头像
        saveAvatar(groupInfo, avatarFile, avatarCover);

    }

    // 修改
    @Override
    public void updateGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
        LambdaQueryWrapper<GroupInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupInfo::getGroupId, groupInfo.getGroupId());
        GroupInfo updateGroup = this.getOne(queryWrapper);
        if (updateGroup == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 判断是否为群组持有人
        if (!updateGroup.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        this.updateById(groupInfo);

        // TODO 更新相关表冗余信息

        // TODO 修改群昵称发送WS消息

        // 保存群头像
        saveAvatar(groupInfo, avatarFile, avatarCover);
    }

    @Override
    public List<MyGroups> getMyGroups(UserTokenDTO userToken) {
        // 获取当前用户的ID
        String userId = userToken.getUserId();

        // 构建查询条件，按创建时间降序排序
        LambdaQueryWrapper<GroupInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupInfo::getGroupOwnerId, userId)
                .orderByDesc(GroupInfo::getCreateTime);

        // 执行查询，获取当前用户创建的群组列表
        List<GroupInfo> groupList = this.list(queryWrapper);
        return BeanUtil.copyToList(groupList, MyGroups.class);
    }

    @Override
    public GroupDetails getGroupInfo(UserTokenDTO userToken, String groupId) {
        // 获取当前用户的ID
        String userId = userToken.getUserId();

        // 判断当前用户是否属于该群聊
        UserContact userContact = userContactService.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userId)
                .eq(UserContact::getContactId, groupId));
        if (null == userContact
                || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())) {
            throw new BusinessException("你不在该群聊或群聊不存在");
        }

        // 查询群信息
        LambdaQueryWrapper<GroupInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupInfo::getGroupId, groupId);
        GroupInfo groupInfo = this.getOne(queryWrapper);

        if (null == groupInfo
                || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 查询该群组里的成员数
        long count = userContactService.count(
                new LambdaQueryWrapper<UserContact>()
                        .eq(UserContact::getContactId, groupId)
                        .eq(UserContact::getStatus, UserContactStatusEnum.FRIEND.getStatus())
        );

        GroupDetails groupDetails = BeanUtil.copyProperties(groupInfo, GroupDetails.class);
        groupDetails.setMemberCount((int) count);

        return groupDetails;
    }

    @Override
    public GroupInfoVO getGroupMember(UserTokenDTO userToken, String groupId) {
        LambdaQueryWrapper<GroupInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupInfo::getGroupId, groupId);
        GroupInfo groupInfo = this.getOne(queryWrapper);
        if (null == groupInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 1.获取所有联系人信息
        List<UserContact> contactList = userContactService.list(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getContactId, groupId)
                .orderByAsc(UserContact::getCreateTime));
        // 2.增加昵称和性别
        List<UserContactVO> contactVOList = contactList.stream().map(contact -> {
            UserContactVO userContactVO = BeanUtil.copyProperties(contact, UserContactVO.class);
            // 获取用户详情信息
            UserInfo userInfo = userInfoService.getById(userContactVO.getUserId());
            userContactVO.setContactName(userInfo.getNickName());
            userContactVO.setSex(userInfo.getSex());
            return userContactVO;
        }).collect(Collectors.toList());

        GroupInfoVO groupInfoVO = new GroupInfoVO();
        groupInfoVO.setGroupInfo(groupInfo);
        groupInfoVO.setUserContactVOList(contactVOList);
        return groupInfoVO;
    }

    private void saveAvatar(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
        if (null == avatarFile) {
            return;
        }
        String baseFolder = commonConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        // 1.拼接头像路径
        String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Constants.IMAGE_SUFFIX;
        // 2.保存原图和缩略图
        avatarFile.transferTo(new File(filePath));
        avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
    }
}
