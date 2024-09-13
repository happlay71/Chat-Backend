package online.happlay.chat.service.impl;

import online.happlay.chat.entity.po.ChatSession;
import online.happlay.chat.mapper.ChatSessionMapper;
import online.happlay.chat.service.IChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话信息 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {

}
