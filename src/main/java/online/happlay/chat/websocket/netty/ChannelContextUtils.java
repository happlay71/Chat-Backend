package online.happlay.chat.websocket.netty;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.po.*;
import online.happlay.chat.entity.vo.WsInitDataVO;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.enums.userContactApply.UserContactApplyStatusEnum;
import online.happlay.chat.mapper.ChatSessionUserMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.*;
import org.apache.catalina.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static online.happlay.chat.constants.Constants.MIL_LIS_SECONDS_3DAYS_AGO;

/**
 * ws通道工具
 */
@Slf4j
@Component
public class ChannelContextUtils {

    // 放入内存中管理
    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    @Resource
    private IUserInfoService userInfoService;
    
    @Resource
    @Lazy
    private IUserContactApplyService userContactApplyService;

    @Resource
    private IChatMessageService chatMessageService;

    @Resource
    private ChatSessionUserMapper chatSessionUserMapper;

    @Resource
    private RedisComponent redisComponent;

    /**
     * TODO 未测试，用于将 userId 绑定到指定的 Netty Channel 上
     * @param userId
     * @param channel
     */
    public void addContext(String userId, Channel channel) {
        try {
            // 获取通道的 channelId
            String channelId = channel.id().toString();
            log.info("channelId: {}", channelId);
            // AttributeKey 是 Netty 中用于在 Channel 上存储属性的键
            AttributeKey attributeKey = null;
            if (!AttributeKey.exists(channelId)) {
                attributeKey = AttributeKey.newInstance(channelId);
            } else {
                attributeKey = AttributeKey.valueOf(channelId);
            }
            // 为通道绑定 userId
            channel.attr(attributeKey).set(userId);

            // 存入群组
            List<String> userContactList = redisComponent.getUserContactList(userId);
            for (String groupId : userContactList) {
                if (groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())) {
                    addToGroup(groupId, channel);
                }
            }

            USER_CONTEXT_MAP.put(userId, channel);
            redisComponent.saveHeartBeat(userId);

            // 更新用户最后连接时间
            UserInfo user = userInfoService.getById(userId);
            if (user != null) {
                user.setLastLoginTime(LocalDateTime.now());
                userInfoService.updateById(user);
            } else {
                log.warn("未找到用户: {}", userId);
            }

            // 给用户发送消息
            Long sourceLastOfTime = user.getLastOffTime();
            Long lastOffTime = sourceLastOfTime;
            if (sourceLastOfTime != null && System.currentTimeMillis() - MIL_LIS_SECONDS_3DAYS_AGO > sourceLastOfTime) {
                lastOffTime = MIL_LIS_SECONDS_3DAYS_AGO;
            }

            /**
             * 1.查询所有会话信息
             */

            List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.selectChatSessions(userId);

            WsInitDataVO wsInitVO = new WsInitDataVO();
            wsInitVO.setChatSessionList(chatSessionUserList);

            /**
             * 2.查询聊天记录-从redis获取
             */
            List<String> groupIdList = userContactList.stream()
                    .filter(item -> item.startsWith(UserContactTypeEnum.GROUP.getPrefix())).collect(Collectors.toList());

            if (!groupIdList.contains(userId)) {
                groupIdList.add(userId); // 将自己作为接收人id填入集合
            }

            // 根据集合的id查询会话信息
            LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(ChatMessage::getContactId, groupIdList);
            queryWrapper.ge(ChatMessage::getSendTime, lastOffTime);
            List<ChatMessage> chatMessageList = chatMessageService.list(queryWrapper);

            wsInitVO.setChatMessageList(chatMessageList);

            /**
             * 3.查询好友申请-在离线后发送的未处理的申请信息
             */
            LambdaQueryWrapper<UserContactApply> applyQuery = new LambdaQueryWrapper<>();
            applyQuery.eq(UserContactApply::getReceiveUserId, userId)
                    .eq(UserContactApply::getStatus, UserContactApplyStatusEnum.INIT.getStatus())
                    .ge(UserContactApply::getLastApplyTime, lastOffTime);

            Integer count = Math.toIntExact(userContactApplyService.count(applyQuery));
            wsInitVO.setApplyCount(count);

            // 发送消息
            MessageSendDTO messageSendDTO = new MessageSendDTO();
            messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
            messageSendDTO.setContactId(userId);
            messageSendDTO.setExtendData(wsInitVO);

            sendMsg(messageSendDTO, userId);
        } catch (Exception e) {
            log.error("链接初始化失败", e);
        }
    }

    /**
     * 发送消息
     * @param messageSendDTO
     * @param receiveId 接收消息的用户的id
     */
    private void sendMsg(MessageSendDTO messageSendDTO, String receiveId) {
        // TODO 26-17:41 JSONUTILS

        Channel userChannel = USER_CONTEXT_MAP.get(receiveId);
        if (userChannel == null) {
            return;
        }

        if (MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDTO.getMessageType())) {
            // 向自己发送信息
            UserInfo userInfo = (UserInfo) messageSendDTO.getExtendData();
            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDTO.setContactId(userInfo.getUserId());
            messageSendDTO.setContactName(userInfo.getNickName());
            messageSendDTO.setExtendData(null);
        } else {
            /**
             * 在一对一聊天的场景下，contactId 不再是用来表示接收方的，
             * 而是用来标识与这条消息相关的最重要联系人，也就是发送者。
             *
             * 站在客户端的角度，好友的联系人就是该用户
             */
            messageSendDTO.setContactId(messageSendDTO.getSendUserId());
            messageSendDTO.setContactName(messageSendDTO.getSendUserNickName());
        }

        userChannel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(messageSendDTO)));
    }


    // 添加进群聊通道
    private void addToGroup(String groupId, Channel channel) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null) {
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }

        if (channel == null) {
            return;
        }

        group.add(channel);
    }

    /**
     * 删除通道
     * @param channel
     */
    public void removeContext(Channel channel) {
        // 获取userId
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        // TODO 判空？
        if (!StrUtil.isEmpty(userId)) {
            USER_CONTEXT_MAP.remove(userId);
        }
        // 移除心跳
        redisComponent.removeUserHeartBeat(userId);
        // 更新用户离线时间
        UserInfo user = userInfoService.getById(userId);
        if (user != null) {
            LocalDateTime time = LocalDateTime.now();
            user.setLastOffTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            userInfoService.updateById(user);
        } else {
            log.warn("未找到用户: {}", userId);
        }
    }

    /**
     * 发送消息
     * @param messageSendDTO
     */
    public void sendMessage(MessageSendDTO messageSendDTO) {
        // 获取id前缀，判断是否是群聊
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDTO.getContactId());
        switch (contactTypeEnum) {
            case USER:
                sendToUser(messageSendDTO);
                break;
            case GROUP:
                sendToGroup(messageSendDTO);
                break;
        }
    }

    // 发送给群组
    private void sendToGroup(MessageSendDTO messageSendDTO) {
        if (StrUtil.isEmpty(messageSendDTO.getContactId())) {
            return;
        }

        ChannelGroup channelGroup = GROUP_CONTEXT_MAP.get(messageSendDTO.getContactId());
        if (channelGroup == null) {
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(messageSendDTO)));
    }

    // 发送给用户
    private void sendToUser(MessageSendDTO messageSendDTO) {
        String contactId = messageSendDTO.getContactId();
        if (StrUtil.isEmpty(contactId)) {
            return;
        }
        sendMsg(messageSendDTO, contactId);

        // 强制下线
        if (MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDTO.getMessageType())) {
            closeContext(contactId);
        }
    }

    /**
     * 强制下线，删除token缓存
     * @param userId
     */
    public void closeContext(String userId) {
        if (StrUtil.isEmpty(userId)) {
            return;
        }
        redisComponent.cleanUserTokenById(userId);
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel == null) {
            return;
        }
        channel.close();
    }

    // TODO 解释
    public void addUserToGroup(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        addUserToGroup(groupId, channel);
    }

    private void addUserToGroup(String groupId, Channel channel) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null) {
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }
        if (channel == null) {
            return;
        }
        group.add(channel);
    }

}
