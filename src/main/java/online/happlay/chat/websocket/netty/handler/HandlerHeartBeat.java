package online.happlay.chat.websocket.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理心跳事件
 * 支持对 Channel 的入站和出站事件进行处理
 */
@Slf4j
public class HandlerHeartBeat extends ChannelDuplexHandler {

    /**
     * Netty 中用于处理用户自定义事件的方法
     * 当有用户事件（evt）发生时，这个方法会被调用。
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        /**
         * IdleStateEvent表示通道空闲状态的事件，例如在心跳机制中，通道没有任何读写操作时会触发这个事件
         */
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
                String userId = attribute.get();
                log.info("用户{}心跳超时", userId);
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                /**
                 * 写超时
                 * 如果是 WRITER_IDLE 状态，表示在指定时间内没有向通道写入任何数据。
                 * 这里通过 ctx.writeAndFlush("heart"); 向通道写入并立即发送一个 "heart" 消息，表示发送心跳包。这是常见的做法，用来维持连接活跃。
                 */
                ctx.writeAndFlush("heart");
            }
        }
    }
}


