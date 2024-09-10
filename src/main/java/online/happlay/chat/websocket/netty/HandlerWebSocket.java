package online.happlay.chat.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单处理器，专门用来处理特定类型的消息帧
 * TextWebSocketFrame，即文本消息帧
 */
@Slf4j
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接断开……");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
         log.info("有新的连接加入……");
    }

    /**
     * 服务器接收到来自客户端的 WebSocket 消息
     * ChannelHandlerContext channelHandlerContext 是处理消息的上下文，提供了对通道和管道的访问。
     * TextWebSocketFrame textWebSocketFrame 是接收到的文本消息帧，包含了实际的文本消息内容。
     * @param channelHandlerContext
     * @param textWebSocketFrame
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

        Channel channel = channelHandlerContext.channel();
        log.info("收到消息{}", textWebSocketFrame.text());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
