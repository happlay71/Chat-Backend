package online.happlay.chat.websocket.netty;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.vo.WsInitDataVO;
import online.happlay.chat.entity.po.ChatSessionUser;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.enums.MessageTypeEnum;
import online.happlay.chat.enums.UserContactTypeEnum;
import online.happlay.chat.mapper.ChatSessionUserMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IChatSessionUserService;
import online.happlay.chat.service.IUserInfoService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static online.happlay.chat.constants.Constants.MIL_LIS_SECONDS_3DAYS_AGO;

@Slf4j
@Component
public class ChannelContextUtils {

    // 放入内存中管理
    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private IChatSessionUserService chatSessionUserService;

    @Resource
    private ChatSessionUserMapper chatSessionUserMapper;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 用于将 userId 绑定到指定的 Netty Channel 上
     * @param userId
     * @param channel
     */
    public void addContext(String userId, Channel channel) {
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
        Long lastOffTime = user.getLastOffTime();
        if (lastOffTime != null && System.currentTimeMillis() - MIL_LIS_SECONDS_3DAYS_AGO > lastOffTime) {
            lastOffTime = MIL_LIS_SECONDS_3DAYS_AGO;
        }

        /**
         * 1.查询所有会话信息
         */

        List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.selectChatSessions(userId);

        WsInitDataVO wsInitVO = new WsInitDataVO();
        wsInitVO.setChatSessionUserList(chatSessionUserList);

        /**
         * 2.查询聊天记录
         */

        /**
         * 3.查询好友申请
         */

        // 发送消息
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDTO.setContactId(userId);
        messageSendDTO.setExtendData(wsInitVO);

        sendMsg(messageSendDTO, userId);
    }

    /**
     * TODO 发送消息
     * @param messageSendDTO
     * @param receiveId 接收消息的用户的id
     */
    private static void sendMsg(MessageSendDTO messageSendDTO, String receiveId) {
        // TODO 26-17:41 JSON
    }


    /**
     * 添加进群聊通道
     * @param groupId
     * @param channel
     */
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

}
