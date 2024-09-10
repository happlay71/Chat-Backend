package online.happlay.chat.websocket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

// TODO netty待学习
@Slf4j
public class NettyWebSocketStarter {

    // 专门用于接受客户端连接的线程组
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // 用于处理已连接的客户端的 I/O 操作
    private static EventLoopGroup workGroup = new NioEventLoopGroup();

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // 为服务器配置两个线程组，一个处理连接请求，另一个处理 I/O 操作
        serverBootstrap.group(bossGroup, workGroup);
        try {
            // 指定服务器使用的通道类型，表示服务器使用 TCP（传输控制协议）
            serverBootstrap.channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new ChannelInitializer() {  // 配置子通道（即每个连接）的处理器
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            // 处理 HTTP 请求的编解码器，用于将 HTTP 请求转换为 HttpRequest 对象，以及将响应转换为 HTTP 响应报文
                            pipeline.addLast(new HttpServerCodec());
                            // 将 HTTP 的多个部分聚合成完整的 HTTP 消息
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            // 用于检测通道的空闲状态
                            pipeline.addLast(new IdleStateHandler(6, 0, 0, TimeUnit.SECONDS));
                            // 自定义的心跳处理器，用于处理心跳超时事件
                            pipeline.addLast(new HandlerHeartBeat());
                            // 处理 WebSocket 协议的握手、ping/pong 以及关闭帧等。"/ws" 是 WebSocket 的 URI
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast(new HandlerWebSocket());
                        }
                    });
            // 绑定端口并启动服务器
            ChannelFuture channelFuture = serverBootstrap.bind(5051).sync();
            log.info("netty启动成功！");
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.info("netty启动失败……");
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
