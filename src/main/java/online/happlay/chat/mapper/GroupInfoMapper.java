package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.GroupInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.happlay.chat.entity.vo.GroupDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 群 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Mapper
public interface GroupInfoMapper extends BaseMapper<GroupInfo> {

    @Select(
            "select gi.group_id as groupId," +
                    "gi.group_name as groupName," +
                    "gi.group_owner_id as groupOwnerId," +
                    "gi.create_time as createTime," +
                    "gi.join_type as joinType," +
                    "gi.status as status, " +
                    "ui.nick_name as groupOwnerNickName, " +
                    "coalesce(count(uc.user_id), 0) as memberCount " +
            "from group_info gi " +
            "left join user_info ui on gi.group_owner_id = ui.user_id " +
            "left join user_contact uc on gi.group_id = uc.contact_id " +
            "where " +
                    "(#{groupId} is null or gi.group_id like concat('%', #{groupId}, '%')) and" +
                    "(#{groupName} is null or gi.group_name like concat('%', #{groupName}, '%')) and" +
                    "(#{groupOwnerId} is null or gi.group_owner_id like concat('%', #{groupOwnerId}, '%')) " +
            "group by gi.group_id, gi.group_name, gi.group_owner_id, gi.create_time, gi.join_type, gi.status, ui.nick_name " +
            "order by gi.create_time desc " +
            "limit #{offset}, #{pageSize}"
    )
    List<GroupDetails> loadGroupWithDetails(
            @Param("groupId") String groupId,
            @Param("groupName") String groupName,
            @Param("groupOwnerId") String groupOwnerId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Select("select count(*) from group_info")
    int countTotalGroups();

}
