package online.happlay.chat.websocket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.websocket.netty.handler.HandlerHeartBeat;
import online.happlay.chat.websocket.netty.handler.HandlerWebSocket;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

// TODO netty待学习
@Slf4j
@Component
public class NettyWebSocketStarter implements Runnable{

    // 专门用于接受客户端连接的线程组
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // 用于处理已连接的客户端的 I/O 操作
    private static EventLoopGroup workGroup = new NioEventLoopGroup();

    @Resource
    private HandlerWebSocket handlerWebSocket;

    @Resource
    private CommonConfig commonConfig;

    @PreDestroy
    public void close() {
        // shutdownGracefully 用来确保在关闭时不会立即中断正在处理的任务，而是允许它们完成
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

    /**
     * 另起一个线程，防止阻塞主线程
     */
    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 为服务器配置两个线程组，一个处理连接请求，另一个处理 I/O 操作
            serverBootstrap.group(bossGroup, workGroup);
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
                            pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                            // 自定义的心跳处理器，用于处理心跳超时事件
                            pipeline.addLast(new HandlerHeartBeat());
                            /**
                             * 处理 WebSocket 协议的握手、ping/pong 以及关闭帧等。
                             * "/ws" 是 WebSocket 的 URI
                             * null 子协议（subprotocols）的集合，指定了服务器支持的 WebSocket 子协议
                             * true 是否允许扩展，以增强通信能力
                             * 64 * 1024 允许的最大帧（Frame）大小，单位是字节
                             * true 表示是否检查来自客户端的 Mask，WebSocket 客户端发送的数据帧需要被掩码，服务器通过该参数来验证数据帧是否符合协议
                             * true 表示是否自动关闭 WebSocket 连接
                             * 10000L 表示 WebSocket 握手的超时时间，以毫秒为单位
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true,
                                    64 * 1024, true, true, 10000L));
                            pipeline.addLast(handlerWebSocket);
                        }
                    });
            // 绑定端口并启动服务器
            ChannelFuture channelFuture = serverBootstrap.bind(commonConfig.getWsPort()).sync();
            log.info("netty启动成功！端口：{}", commonConfig.getWsPort());
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.info("netty启动失败……");
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

}
