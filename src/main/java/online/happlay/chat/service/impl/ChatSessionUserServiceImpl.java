package online.happlay.chat.service.impl;

import online.happlay.chat.entity.po.ChatSessionUser;
import online.happlay.chat.mapper.ChatSessionUserMapper;
import online.happlay.chat.service.IChatSessionUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话用户 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Service
public class ChatSessionUserServiceImpl extends ServiceImpl<ChatSessionUserMapper, ChatSessionUser> implements IChatSessionUserService {

}
