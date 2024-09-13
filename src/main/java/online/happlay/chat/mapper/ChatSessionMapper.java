package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 会话信息 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

}
