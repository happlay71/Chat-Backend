package online.happlay.chat.service.impl;

import online.happlay.chat.entity.po.ChatMessage;
import online.happlay.chat.mapper.ChatMessageMapper;
import online.happlay.chat.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

}
