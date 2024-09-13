package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 聊天消息表 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

}
