package online.happlay.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.po.ChatSessionUser;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.mapper.ChatSessionUserMapper;
import online.happlay.chat.service.IChatSessionUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserContactService;
import online.happlay.chat.websocket.netty.MessageHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 会话用户 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Service
@RequiredArgsConstructor
public class ChatSessionUserServiceImpl extends ServiceImpl<ChatSessionUserMapper, ChatSessionUser> implements IChatSessionUserService {

    @Lazy
    @Resource
    private IUserContactService userContactService;

    private final MessageHandler messageHandler;

    private final ChatSessionUserMapper chatSessionUserMapper;

    @Override
    public List<ChatSessionUser> getByContactId(String contactId) {
        LambdaQueryWrapper<ChatSessionUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionUser::getContactId, contactId);
        return this.list(queryWrapper);
    }

    @Override
    public void updateNameByContactId(String contactId, String contactName) {
        /**
         * new ChatSessionUser()：这是一个空的 ChatSessionUser 对象，它在 update 方法中用来指定更新的字段。
         * new UpdateWrapper<>()：我们使用 UpdateWrapper 来指定更新条件以及更新的字段。
         * set("contact_name", contactName)：这指定了要更新的字段 contact_name 和其新值 contactName。
         * eq("contact_id", contactId)：通过 eq 方法，我们指定条件，所有 contact_id 等于 contactId 的记录都会被更新。
         */
        chatSessionUserMapper.update(
                new ChatSessionUser(),
                new UpdateWrapper<ChatSessionUser>()
                        .set("contact_name", contactName)
                        .eq("contact_id", contactId)
        );

        // 获取联系人类型
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

        switch (contactTypeEnum) {
            case USER:
                LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserContact::getContactId, contactId)
                        .eq(UserContact::getContactType, contactTypeEnum)
                        .eq(UserContact::getStatus, UserContactStatusEnum.FRIEND.getStatus());
                List<UserContact> userContactList = userContactService.list(queryWrapper);
                userContactList.forEach(userContact -> {
                    // 修改个人昵称向所有好友发送WS消息
                    MessageSendDTO messageSendDTO = new MessageSendDTO<>();
                    messageSendDTO.setContactType(contactTypeEnum.getType());
                    messageSendDTO.setContactId(userContact.getUserId());
                    messageSendDTO.setExtendData(contactName);
                    messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
                    // 发消息需要发送人id和昵称
                    messageSendDTO.setSendUserId(contactId);
                    messageSendDTO.setSendUserNickName(contactName);
                    messageHandler.sendMessage(messageSendDTO);
                });

                break;
            case GROUP:
                // 修改群昵称发送WS消息
                MessageSendDTO messageSendDTO = new MessageSendDTO<>();
                messageSendDTO.setContactType(contactTypeEnum.getType());
                messageSendDTO.setContactId(contactId);
                messageSendDTO.setExtendData(contactName);
                messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
                messageHandler.sendMessage(messageSendDTO);
                break;
        }
    }
}
