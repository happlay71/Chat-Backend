package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.UserContactApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.happlay.chat.entity.vo.userContactApply.UserContactApplyLoadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 联系人申请 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Mapper
public interface UserContactApplyMapper extends BaseMapper<UserContactApply> {

    @Select(
        "select uca.*, " +
                "case when uca.contact_type = 0 then ui.nick_name " +
                "     when uca.contact_type = 1 then gi.group_name " +
                "     else  null " +
                "end  as contactName " +
                "from user_contact_apply uca " +
                "left join user_info ui on uca.contact_type = 0 and uca.contact_id collate utf8mb4_general_ci = ui.user_id " +
                "left join group_info gi on uca.contact_type = 1 and uca.contact_id collate utf8mb4_general_ci = gi.group_id " +
                "where uca.receive_user_id = #{userId} " +
                "order by uca.last_apply_time desc " +
                "LIMIT #{offset}, #{pageSize}"
    )
    List<UserContactApplyLoadVO> findWithContactName(@Param("userId") String userId, @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("select  count(*) from user_contact_apply uca where uca.receive_user_id = #{userId}")
    int countByReceiveUserId(@Param("userId") String userId);
}
