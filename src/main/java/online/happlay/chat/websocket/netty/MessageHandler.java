package online.happlay.chat.websocket.netty;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 消息处理器
 */
@Slf4j
@Component
public class MessageHandler {

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    /**
     * 一种 发布/订阅模式（Pub/Sub），通过 Redisson 实现的分布式消息监听
     * @PostConstruct 服务启动时监听
     */
    @PostConstruct
    public void listenMessage() {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.addListener(MessageSendDTO.class, (MessageSendDTO, sendDTO) -> {
            log.info("接收广播消息：{}", JSONUtil.toJsonStr(sendDTO));  // 转换成json输出
            channelContextUtils.sendMessage(sendDTO);
        });
    }

    public void sendMessage(MessageSendDTO messageSendDTO) {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(messageSendDTO);
    }
}
