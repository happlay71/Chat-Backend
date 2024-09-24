package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.group.LoadGroupQueryDTO;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.*;
import online.happlay.chat.entity.vo.group.GroupDetails;
import online.happlay.chat.entity.vo.group.GroupInfoVO;
import online.happlay.chat.entity.vo.group.MyGroups;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.entity.vo.userContact.UserContactVO;
import online.happlay.chat.enums.group.GroupOpTypeEnum;
import online.happlay.chat.enums.group.GroupStatusEnum;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.enums.message.MessageStatusEnum;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.GroupInfoMapper;
import online.happlay.chat.mapper.UserContactMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.utils.StringTools;
import online.happlay.chat.websocket.netty.ChannelContextUtils;
import online.happlay.chat.websocket.netty.MessageHandler;
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

    @Lazy
    @Resource
    private IGroupInfoService groupInfoService;

    private final IChatSessionService chatSessionService;

    private final IChatMessageService chatMessageService;

    private final IChatSessionUserService chatSessionUserService;

    private final GroupInfoMapper groupInfoMapper;

    private final UserContactMapper userContactMapper;

    private final ChannelContextUtils channelContextUtils;

    private final MessageHandler messageHandler;

    /**
     * 新增群组
     * TODO 目前为群主建群，再邀人，可将邀人和建群操作合并
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     * @throws IOException
     */
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

        LocalDateTime time = LocalDateTime.now();
        groupInfo.setCreateTime(time);
        groupInfo.setGroupId(StringTools.getGroupId());
        this.save(groupInfo);

        // 将群组添加到联系人数据库
        UserContact userContact = new UserContact();
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContact.setContactType(UserContactTypeEnum.GROUP.getType());
        userContact.setContactId(groupInfo.getGroupId());
        userContact.setUserId(groupInfo.getGroupOwnerId());
        userContact.setCreateTime(time);
        userContactService.save(userContact);

        // 1.创建会话
        // 1.1会话信息
        String sessionId = StringTools.getChatSessionIdForGroup(groupInfo.getGroupId());
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
        chatSessionService.saveOrUpdate(chatSession);

        // 1.2会话用户
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
        chatSessionUser.setContactId(groupInfo.getGroupId());
        chatSessionUser.setContactName(groupInfo.getGroupName());
        chatSessionUser.setSessionId(sessionId);
        chatSessionUserService.save(chatSessionUser);

        // 1.3聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
        chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
        chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatMessage.setContactId(groupInfo.getGroupId());
        chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDEND.getStatus());
        chatMessageService.save(chatMessage);

        // 存入redis
        redisComponent.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

        // 将群主用户通道加入该群聊通道
        channelContextUtils.addUserToGroup(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

        // 发送初始的ws消息
        chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
        chatSessionUser.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSessionUser.setMemberCount(1);  // 只有群主

        MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);
        messageSendDTO.setExtendData(chatSessionUser);
        messageSendDTO.setLastMessage(chatSessionUser.getLastMessage());
        messageHandler.sendMessage(messageSendDTO);


        // 保存群头像
        saveAvatar(groupInfo, avatarFile, avatarCover);

    }

    /**
     * 修改群组信息
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     * @throws IOException
     */
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

        // 1.更新相关表冗余信息
        String contactNameUpdate = null;
        if (!updateGroup.getGroupName().equals(groupInfo.getGroupName())) {
            contactNameUpdate = groupInfo.getGroupName();
        }
        if (contactNameUpdate == null) {
            return;
        }

        // 根据群组id修改会话用户表里的群组昵称，并发送ws消息
        chatSessionUserService.updateNameByContactId(groupInfo.getGroupId(), groupInfo.getGroupName());


        // 保存群头像
        saveAvatar(groupInfo, avatarFile, avatarCover);
    }

    /**
     * 获取该用户创建的群组
     * @param userToken
     * @return
     */
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

    /**
     * 获取群组信息
     * @param userToken
     * @param groupId
     * @return
     */
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

    /**
     * 获取群成员信息
     * @param userToken
     * @param groupId
     * @return
     */
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

    /**
     * 获取群组信息，内联查询群主信息及群人数
     * @param loadGroupQueryDTO
     * @return
     */
    @Override
    public PaginationResultVO<GroupDetails> loadGroup(LoadGroupQueryDTO loadGroupQueryDTO) {
        // 获取当前页码和每页大小
        Integer pageNo = loadGroupQueryDTO.getPageNo();
        Integer pageSize = loadGroupQueryDTO.getPageSize();

        // 计算分页的偏移量
        int offset = (pageNo - 1) * pageSize;

        // 查询群组信息及其相关联的信息，如群主昵称、成员数
        List<GroupDetails> groupDetailsList = groupInfoMapper.loadGroupWithDetails(
                loadGroupQueryDTO.getGroupId(),
                loadGroupQueryDTO.getGroupName(),
                loadGroupQueryDTO.getGroupOwnerId(),
                offset,
                pageSize
        );

        // 获取总记录数
        int totalCount = groupInfoMapper.countTotalGroups();

        // 计算总页数
        int pageTotal = (int) Math.ceil((double) totalCount / pageSize);

        // 构造 PaginationResultVO 对象并返回
        return new PaginationResultVO<>(
                totalCount,          // 总记录数
                pageSize,            // 每页大小
                pageNo,              // 当前页码
                pageTotal,           // 总页数
                groupDetailsList     // 当前页的数据记录列表
        );
    }

    /**
     * 解释群组
     * @param groupId
     * @param groupOwnerId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolutionGroup(String groupId, String groupOwnerId) {
        GroupInfo groupInfo = this.getById(groupId);
        if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(groupOwnerId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 删除群组
        groupInfo.setStatus(GroupStatusEnum.DISSOLUTION.getStatus());
        this.updateById(groupInfo);

        // 更新联系人信息
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(UserContact::getContactId, groupId)
                .eq(UserContact::getContactType, UserContactTypeEnum.GROUP.getType());

        // 将所有搜索结果的状态设为del
        UserContact userContact = new UserContact();
        userContact.setStatus(UserContactStatusEnum.DEL.getStatus());
        userContactService.update(userContact, queryWrapper);

        // 移除相关群员的联系人缓存
        List<UserContact> userContactList = userContactService.list(queryWrapper);
        userContactList.forEach(contact -> redisComponent.removeUserContact(contact.getUserId(), contact.getContactId()));

        String sessionId = StringTools.getChatSessionIdForGroup(groupId);
        LocalDateTime time = LocalDateTime.now();
        String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();

        // 1.更新会话信息
        ChatSession chatSession = chatSessionService.getById(sessionId);
        chatSession.setLastMessage(messageContent);
        chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSession.setSessionId(sessionId);
        chatSessionService.updateById(chatSession);

        // 2.记录群消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDEND.getStatus());
        chatMessage.setMessageType(MessageTypeEnum.DISSOLUTION_GROUP.getType());
        chatMessage.setContactId(groupId);
        chatMessage.setMessageContent(messageContent);
        chatMessageService.save(chatMessage);

        // 3.发送解散通知消息
        MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);
        messageHandler.sendMessage(messageSendDTO);
    }

    /**
     * 拉人或踢人
     * @param userToken
     * @param groupId
     * @param selectContacts
     * @param opType
     */
    @Override
    public void addOrRemoveGroupUser(UserTokenDTO userToken, String groupId, String selectContacts, Integer opType) {
        GroupInfo groupInfo = this.getById(groupId);
        // 群组不存在或不属于该用户
        if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(userToken.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 要拉入或踢出的用户的id
        String[] contactIdList = selectContacts.split(",");
        for (String contactId : contactIdList) {
            // 判断是否为用户而非群组
            Integer contactType = UserContactTypeEnum.getByPrefix(contactId).getType();
            if (contactType == null) {
                throw new BusinessException(600, "此用户非有效用户");
            }
            if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }

            GroupOpTypeEnum opTypeEnum = GroupOpTypeEnum.getByOpType(opType);
            switch (opTypeEnum) {
                case ZERO:
                    // 内部调用事务不会生效
                    groupInfoService.leaveGroup(contactId, groupId, MessageTypeEnum.REMOVE_GROUP);
                    break;
                case ONE:
                    userContactService.addContact(contactId, null, groupId, UserContactTypeEnum.GROUP.getType(), null);
                    break;
            }
        }
    }

    /**
     * 离开群聊-----内部调用此方法(通过this)会使事务失效
     * @param userId
     * @param groupId
     * @param messageTypeEnum
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) {
        GroupInfo groupInfo = this.getById(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 群主无法退群
        if (userId.equals(groupInfo.getGroupOwnerId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        QueryWrapper<UserContact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("groupId", groupId);
        int count = userContactMapper.delete(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        UserInfo userInfo = userInfoService.getById(userId);
        String sessionId = StringTools.getChatSessionIdForGroup(groupId);
        LocalDateTime time = LocalDateTime.now();
        String messageContent = String.format(messageTypeEnum.getInitMessage(), userInfo.getNickName());

        // 会话信息表
        ChatSession chatSession = chatSessionService.getById(sessionId);
        chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSession.setLastMessage(messageContent);
        chatSessionService.updateById(chatSession);

        // 聊天消息表
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDEND.getStatus());
        chatMessage.setMessageType(messageTypeEnum.getType());
        chatMessage.setContactId(groupId);
        chatMessage.setMessageContent(messageContent);
        chatMessageService.save(chatMessage);

        // 更新群聊人数
        long memberCount = userContactService.count(new QueryWrapper<UserContact>()
                .eq("contact_id", groupId)
                .eq("status", UserContactStatusEnum.FRIEND.getStatus()));

        // 发送消息
        MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);
        messageSendDTO.setExtendData(userId);
        messageSendDTO.setMemberCount((int) memberCount);
        messageHandler.sendMessage(messageSendDTO);
    }

    /**
     * 保存头像信息
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     * @throws IOException
     */
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
