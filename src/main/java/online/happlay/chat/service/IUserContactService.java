package online.happlay.chat.service;

import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.vo.UserContactSearchResultVO;
import online.happlay.chat.entity.po.UserContact;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 联系人 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
public interface IUserContactService extends IService<UserContact> {

    UserContactSearchResultVO searchContact(String userId, String contactId);

    Integer applyAdd(UserTokenDTO userToken, String contactId, String applyInfo);
}
