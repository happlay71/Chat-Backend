package online.happlay.chat.service;

import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * <p>
 * 聊天消息表 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
public interface IChatMessageService extends IService<ChatMessage> {
    MessageSendDTO saveMessage(ChatMessage chatMessage, UserTokenDTO userToken);

    void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover);

    File downloadFile(UserTokenDTO userToken, long messageId, Boolean showCover);
}
