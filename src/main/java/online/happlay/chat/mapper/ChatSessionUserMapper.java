package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.ChatSessionUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    @Select(
            "select csu.*, cs.last_receive_time, cs.last_message, " +
            "count(uc.contact_id) as memberCount " +
            "from chat_session_user csu " +
            "join chat_session cs on cs.session_id = csu.session_id " +
            "left join user_contact uc on csu.contact_id = uc.contact_id and uc.contact_type = 1 " +
            "where csu.user_id = #{userId} " +
            "group by csu.session_id, cs.last_receive_time, csu.contact_id, cs.last_message, csu.user_id " +
            "order by cs.last_receive_time desc"
    )
    List<ChatSessionUser> selectChatSessions(@Param("userId") String userId);  // 不是群组的memberCount为0
}
