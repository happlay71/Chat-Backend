package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.*;
import online.happlay.chat.entity.vo.userContact.UserContactSearchResultVO;
import online.happlay.chat.entity.vo.user.UserInfoVO;
import online.happlay.chat.entity.vo.user.UserLoadContactVO;
import online.happlay.chat.enums.*;
import online.happlay.chat.enums.group.GroupStatusEnum;
import online.happlay.chat.enums.message.MessageStatusEnum;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.JoinTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.enums.userContactApply.UserContactApplyStatusEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserContactMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.utils.StringTools;
import online.happlay.chat.websocket.netty.ChannelContextUtils;
import online.happlay.chat.websocket.netty.MessageHandler;
import org.apache.catalina.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 联系人 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Service
@RequiredArgsConstructor
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact> implements IUserContactService {

    private final IUserInfoService userInfoService;

    private final ChannelContextUtils channelContextUtils;

    private final IChatSessionUserService chatSessionUserService;

    private final IChatSessionService chatSessionService;

    private final IChatMessageService chatMessageService;

    private final RedisComponent redisComponent;

    private final MessageHandler messageHandler;




    @Resource
    @Lazy
    private IGroupInfoService groupInfoService;

    @Override
    public UserContactSearchResultVO searchContact(String userId, String contactId) {
        // 1.获取id前缀
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (typeEnum == null) {
            return null;
        }
        UserContactSearchResultVO resultDto = new UserContactSearchResultVO();
        // 2.判断是人还是群组
        switch (typeEnum) {
            case USER:
                UserInfo userInfo = userInfoService.getById(contactId);
                if (userInfo == null) {
                    return null;
                }
                resultDto = BeanUtil.copyProperties(userInfo, UserContactSearchResultVO.class);
                break;
            case GROUP:
                GroupInfo groupInfo = groupInfoService.getById(contactId);
                if (groupInfo == null) {
                    return null;
                }
                resultDto.setNickName(groupInfo.getGroupName());
                break;
        }

        // 设置通用属性
        resultDto.setContactId(contactId);
        resultDto.setContactType(typeEnum.toString());

        // 判断是否是自己
        if (userId.equals(contactId)) {
            resultDto.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            return resultDto;
        }

        // 查询是否是好友
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, userId).eq(UserContact::getContactId, contactId);
        UserContact userContact = this.getOne(queryWrapper);
        resultDto.setStatus(userContact == null ? null : userContact.getStatus());
        return resultDto;
    }

    @Override
    public List<UserLoadContactVO> loadContact(String userId, UserContactTypeEnum typeEnum) {
        // TODO 展示自己创建的群，加入的群，联系人（只展示好友，被删除，被拉黑的（初次申请除外））

        // 查询所有符合条件的user_contact的内容
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, userId)
                .ne(UserContact::getStatus, UserContactStatusEnum.NOT_FRIEND.getStatus())
                .ne(UserContact::getStatus, UserContactStatusEnum.DEL.getStatus())
                .ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST.getStatus());

        switch (typeEnum) {
            case USER:
                // 查询所有符合条件的个人联系人
                queryWrapper.eq(UserContact::getContactType, UserContactTypeEnum.USER.getType());
                // 如果创建时间和最后更新时间相等且状态为被拉黑的情况，则不出现在查询结果里
                queryWrapper.and(qw ->
                                qw.ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST_BE.getStatus())
                                        .or()
                                        .apply("create_time <> last_update_time"))
                        .orderByDesc(UserContact::getLastUpdateTime);
                break;
            case GROUP:
                // 查询所有符合条件的群组联系人
                queryWrapper.eq(UserContact::getContactType, UserContactTypeEnum.GROUP.getType());
                System.out.println("群组" + this.list(queryWrapper));
                // 如果创建时间和最后更新时间相等且状态为被拉黑的情况，则不出现在查询结果里
                // 这里的意思是只要创建时间和最后更新时间不等或不为被拉黑的情况，则包含在查询记录中
                queryWrapper.and(qw ->
                        qw.ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST_BE.getStatus())
                                .or()
                                .apply("create_time <> last_update_time"))
                        .orderByDesc(UserContact::getLastUpdateTime);
                System.out.println("群组" + this.list(queryWrapper));
                break;
        }

        List<UserContact> userContacts = this.list(queryWrapper);

        List<UserLoadContactVO> collectList = userContacts.stream().map(userContact -> {
            UserLoadContactVO userLoadContactVO = BeanUtil.copyProperties(userContact, UserLoadContactVO.class);
            String contactName = null;
            switch (typeEnum) {
                case USER:
                    // 考虑到机器人不会加入到用户信息表里，所以需要先判断是否为机器人
                    if (userLoadContactVO.getContactId().equals("Urobot")) {
                        contactName = "Urobot";
                    } else {
                        contactName = userInfoService.getById(userLoadContactVO.getContactId()).getNickName();
                    }
                    break;
                case GROUP:
                    contactName = groupInfoService.getById(userLoadContactVO.getContactId()).getGroupName();
                    break;
            }
            userLoadContactVO.setContactName(contactName);
            return userLoadContactVO;
        }).collect(Collectors.toList());

        return collectList;
    }

    @Override
    public UserInfoVO getContactInfo(UserTokenDTO userToken, String contactId) {
        UserInfo userInfo = userInfoService.getById(contactId);
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        // 初始化为非好友
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        // 在联系人中查找状态
        // 原操作为如果在联系人中存在则设置为FRIEND，修改为设置成联系人数据库中的状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userToken.getUserId())
                .eq(UserContact::getContactId, contactId));
        if (userContact != null) {
            userInfoVO.setContactStatus(userContact.getStatus());
        }

        return userInfoVO;
    }

    @Override
    public void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo) {
        // 群聊人数
        if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
            long count = this.count(
                    new LambdaQueryWrapper<UserContact>()
                            .eq(UserContact::getContactId, contactId)
                            .eq(UserContact::getStatus, UserContactStatusEnum.FRIEND.getStatus())
            );

            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupMemberCount()) {
                throw new BusinessException("成员已满员，无法加入");
            }
        }

        // 同意，双方好友记录写入数据库
        ArrayList<UserContact> contactList = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        // 申请人添加对方
        UserContact userContact = new UserContact();
        userContact.setUserId(applyUserId);
        userContact.setContactId(contactId);
        userContact.setContactType(contactType);
        userContact.setCreateTime(time);
        userContact.setLastUpdateTime(time);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        contactList.add(userContact);

        // 受邀人添加申请人，写入数据库，群组不用记录
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            userContact = new UserContact();
            userContact.setUserId(receiveUserId);
            userContact.setContactId(applyUserId);
            userContact.setContactType(contactType);
            userContact.setCreateTime(time);
            userContact.setLastUpdateTime(time);
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            contactList.add(userContact);
        }

        this.saveBatch(contactList);

        // 如果是好友，接收人也添加申请人为好友 添加缓存

        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            // 在对方角度：将该用户作为好友的好友添加进缓存
            redisComponent.addUserContact(receiveUserId, applyUserId);
        }

        redisComponent.addUserContact(applyUserId, contactId);

        // 创建会话 发送消息
        String sessionId = null;
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            sessionId = StringTools.getChatSessionIdForUser(new String[]{applyUserId, contactId});
        } else {
            sessionId = StringTools.getChatSessionIdForGroup(contactId);
        }

        List<ChatSessionUser> chatSessionUserList = new ArrayList<>();
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            // 用户
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(applyInfo);
            chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            chatSessionService.saveOrUpdate(chatSession);

            // 申请人session
            ChatSessionUser applySessionUser = new ChatSessionUser();
            applySessionUser.setUserId(applyUserId);  // 申请人的id
            applySessionUser.setContactId(contactId);
            applySessionUser.setSessionId(sessionId);
            UserInfo contactUser = userInfoService.getById(contactId);
            applySessionUser.setContactName(contactUser.getNickName());
            chatSessionUserList.add(applySessionUser);

            // 接收人session
            ChatSessionUser contactSessionUser = new ChatSessionUser();
            contactSessionUser.setUserId(contactId);  // 联系人的id
            contactSessionUser.setContactId(applyUserId);
            contactSessionUser.setSessionId(sessionId);
            UserInfo applyUser = userInfoService.getById(applyUserId);
            contactSessionUser.setContactName(applyUser.getNickName());
            chatSessionUserList.add(contactSessionUser);

            chatSessionUserService.saveOrUpdateBatch(chatSessionUserList);

            // 记录消息表
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            chatMessage.setMessageContent(applyInfo);
            chatMessage.setSendUserId(applyUserId);
            chatMessage.setSendUserNickName(applyUser.getNickName());
            chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            chatMessage.setContactId(contactId);
            chatMessage.setContactType(UserContactTypeEnum.USER.getType());
            chatMessageService.save(chatMessage);

            MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);
            // 发送给接收人、申请人
            messageHandler.sendMessage(messageSendDTO);

            // 发送给该用户本身
            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDTO.setContactId(applyUserId);
            messageSendDTO.setExtendData(contactUser);
            messageHandler.sendMessage(messageSendDTO);

        } else {
            // 发送申请的人的角度
            // 1.会话用户
            ChatSessionUser chatSessionUser = new ChatSessionUser();
            chatSessionUser.setUserId(applyUserId);
            chatSessionUser.setContactId(contactId);

            // 1.1获取对应群组信息
            GroupInfo groupInfo = groupInfoService.getById(contactId);

            chatSessionUser.setContactName(groupInfo.getGroupName());
            chatSessionUser.setSessionId(sessionId);
            chatSessionUserService.save(chatSessionUser);

            // 2.会话信息
            // 2.1查询申请人信息及发送的消息
            UserInfo applyUserInfo = userInfoService.getById(applyUserId);
            String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(), applyUserInfo.getNickName());

            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(sendMessage);
            chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            chatSessionService.saveOrUpdate(chatSession);

            // 3.聊天消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
            chatMessage.setMessageContent(sendMessage);
            chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            chatMessage.setContactId(contactId);
            chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
            chatMessage.setStatus(MessageStatusEnum.SENDEND.getStatus());
            chatMessageService.save(chatMessage);

            // 申请人存入redis
            redisComponent.addUserContact(applyUserId, groupInfo.getGroupId());

            // 将申请人通道加入该群聊通道
            channelContextUtils.addUserToGroup(applyUserId, groupInfo.getGroupId());

            // 发送群消息
            MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);
            messageSendDTO.setContactId(contactId);

            // 获取群员数量
            long memberCount = this.count(new LambdaQueryWrapper<UserContact>()
                    .eq(UserContact::getContactId, contactId)
                    .eq(UserContact::getContactType, UserContactStatusEnum.FRIEND.getStatus()));
            messageSendDTO.setMemberCount((int) memberCount);
            messageSendDTO.setContactName(groupInfo.getGroupName());

            messageHandler.sendMessage(messageSendDTO);
        }
    }

    @Override
    @Transactional
    public void removeUserContact(String userId, String contactId, UserContactStatusEnum userContactStatusEnum) {
        // 双条件的主键约束会导致报错，报已存在相同数据
        // 查找并更新当前用户与好友的关系状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userId)
                .eq(UserContact::getContactId, contactId));
        if (userContact == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 只更新状态字段，避免更新其他字段导致唯一约束冲突！！！
        userContact.setStatus(userContactStatusEnum.getStatus());
        this.update(new LambdaUpdateWrapper<UserContact>()
                .set(UserContact::getStatus, userContactStatusEnum.getStatus())
                .eq(UserContact::getUserId, userContact.getUserId())
                .eq(UserContact::getContactId, userContact.getContactId()));

        // 查找并更新好友与当前用户的关系状态
        UserContact otherUserContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, contactId)
                .eq(UserContact::getContactId, userId));
        if (otherUserContact == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 根据当前用户的操作更新好友的状态
        if (UserContactStatusEnum.DEL == userContactStatusEnum) {
            otherUserContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
        } else if (UserContactStatusEnum.BLACKLIST == userContactStatusEnum) {
            otherUserContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
        }
        this.update(new LambdaUpdateWrapper<UserContact>()
                .set(UserContact::getStatus, otherUserContact.getStatus())
                .eq(UserContact::getUserId, otherUserContact.getUserId())
                .eq(UserContact::getContactId, otherUserContact.getContactId()));

        // TODO 从我的列表缓存中删除好友

        // TODO 从好友的列表缓存中删除我
    }

    /**
     * 添加机器人好友
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addContactForRobot(String userId) {
        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        String contactId = sysSettingDTO.getRobotUid();
        String contactName = sysSettingDTO.getRobotNicName();
        String sendMessage = sysSettingDTO.getRobotWelcome();
        sendMessage = cleanHtmlTag(sendMessage);
        // 增加机器人好友
        LocalDateTime time = LocalDateTime.now();
        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(contactId);
        userContact.setContactType(UserContactTypeEnum.USER.getType());
        userContact.setCreateTime(time);
        userContact.setLastUpdateTime(time);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        this.save(userContact);

        // 增加会话信息
        String sessionId = StringTools.getChatSessionIdForUser(new String[]{userId, contactId});
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastMessage(sendMessage);
        chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSessionService.save(chatSession);

        // 增加会话人信息
        // TODO 好像没成功加入到chatSessionUser
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(userId);
        chatSessionUser.setContactId(contactId);
        chatSessionUser.setContactName(contactName);
        chatSessionUser.setSessionId(sessionId);
        chatSessionUserService.save(chatSessionUser);

        // 增加聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
        chatMessage.setSendUserId(contactId);
        chatMessage.setSendUserNickName(contactName);
        chatMessage.setMessageContent(sendMessage);
        chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatMessage.setContactId(userId);  // 对于机器人来说联系人是该用户
        chatMessage.setContactType(UserContactTypeEnum.USER.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDEND.getStatus());
        chatMessageService.save(chatMessage);

    }

    /**
     * 获取好友详细信息
     * @param userToken
     * @param contactId
     * @return
     */
    @Override
    public UserInfoVO getContactUserInfo(UserTokenDTO userToken, String contactId) {

        // 在联系人中查找状态
        // 原操作为如果在联系人中存在则设置为FRIEND，修改为设置成联系人数据库中的状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userToken.getUserId())
                .eq(UserContact::getContactId, contactId));
        if (null == userContact || !ArrayUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
        }, userContact.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getById(contactId);
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);

        return userInfoVO;
    }

    /**
     * 防止HTML注入
     * @param content
     * @return
     */
    public static String cleanHtmlTag(String content) {
        if (StrUtil.isEmpty(content)) {
            return content;
        }
        // 将字符串中的所有 < 替换为 &lt;。这是为了防止 HTML 注入攻击，或者防止浏览器错误地将某些内容识别为 HTML 标签
        content = content.replace("<", "&lt;");
        // 将换行符替换为 <br> 标签
        content = content.replace("\r\n", "<br>");
        content = content.replace("\n", "<br>");
        return content;
    }
}
