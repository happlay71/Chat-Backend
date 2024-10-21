package online.happlay.chat.mapper;

import online.happlay.chat.entity.po.ChatSessionUser;
import online.happlay.chat.entity.po.UserContact;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 联系人 Mapper 接口
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Mapper
public interface UserContactMapper extends BaseMapper<UserContact> {

    void saveOrUpdateList(List<UserContact> userContactList);

}
