package online.happlay.chat.config;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CommonConfig {

    /**
     * websocket 端口
     */
    @Value("${ws.port:}")
    private Integer wsPort;

    /**
     * 文件目录
     */
    @Value("${project.folder:}")
    private String projectFolder;

    /**
     * 超级管理员邮箱
     */
    @Value("${admin.emails:}")
    private String adminEmails;

    public String getProjectFolder() {
        if (StrUtil.isNotBlank(projectFolder) && !projectFolder.endsWith("/")) {
            return projectFolder + "/";
        }
        return projectFolder;
    }
}
