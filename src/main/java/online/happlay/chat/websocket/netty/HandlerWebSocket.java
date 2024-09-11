package online.happlay.chat.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * 用于处理 Netty 中的用户自定义事件。它特别关注 WebSocket 握手完成的事件，并在握手完成后执行一些操作
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // WebSocketServerProtocolHandler.HandshakeComplete 类型的事件表明 WebSocket 的握手已经成功完成
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String url = complete.requestUri();
            String token = getToken(url);
            if (token == null) {
                ctx.channel().close();
                return;
            }
            log.info("url{}", url);
            log.info("token{}", token);
        }
    }

    private String getToken(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query != null) {
                Map<String, String> queryParams = Stream.of(query.split("&"))
                        .map(param -> param.split("="))
                        .collect(Collectors.toMap(parts -> parts[0], parts -> parts.length > 1 ? parts[1] : ""));
                return queryParams.get("token");
            }
        } catch (URISyntaxException e) {
            log.error("URL语法错误：{}", url, e);
        }
        return null;
    }
}
