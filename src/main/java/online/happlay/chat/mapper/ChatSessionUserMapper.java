package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.ChatSessionUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 会话用户 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Mapper
public interface ChatSessionUserMapper extends BaseMapper<ChatSessionUser> {

}
