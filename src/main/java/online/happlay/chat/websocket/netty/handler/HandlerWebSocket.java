package online.happlay.chat.websocket.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.websocket.netty.ChannelContextUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
@ChannelHandler.Sharable
@Component
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接断开……");
        channelContextUtils.removeContext(ctx.channel());
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
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        log.info("收到userId{}消息：{}", userId, textWebSocketFrame.text());
        redisComponent.saveHeartBeat(userId);
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
            UserTokenDTO userTokenDTO = redisComponent.getUserTokenDTO(token);
            if (null == userTokenDTO) {
                ctx.channel().close();
                return;
            }
            channelContextUtils.addContext(userTokenDTO.getUserId(), ctx.channel());
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
