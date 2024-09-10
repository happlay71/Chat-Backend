package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.AppUpdate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * app发布 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-09-08
 */
@Mapper
public interface AppUpdateMapper extends BaseMapper<AppUpdate> {

    @Select(
        "select * " +
        "from app_update " +
        "where version > #{appVersion} and (status = 2 or (status = 1 and find_in_set(#{uid}, grayscale_uid))) " +
        "order by id desc " +
        "limit 0, 1"
    )
    AppUpdate checkVersion(@Param("appVersion") String appVersion, @Param("uid") String uid);
}
