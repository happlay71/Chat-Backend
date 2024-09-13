package online.happlay.chat;

import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.redis.RedisUtils;
import online.happlay.chat.websocket.netty.NettyWebSocketStarter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 用于进行一些启动时的初始化操作
 * 检查数据库连接、Redis 状态，以及启动 Netty WebSocket 服务器
 */
@Slf4j
@Component
public class InitRun implements ApplicationRunner {
    @Resource
    private DataSource dataSource;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            dataSource.getConnection();
            redisUtils.get("test");
            new Thread(nettyWebSocketStarter).start();
            log.info("服务启动成功，准备开发！");
        } catch (SQLException e) {
            log.error("数据库配置错误！");
        } catch (Exception e) {
            log.error("服务启动失败", e);
        }
    }
}
