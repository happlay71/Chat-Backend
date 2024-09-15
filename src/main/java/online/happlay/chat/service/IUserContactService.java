package online.happlay.chat.service;

import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.vo.userContact.UserContactSearchResultVO;
import online.happlay.chat.entity.po.UserContact;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.user.UserInfoVO;
import online.happlay.chat.entity.vo.user.UserLoadContactVO;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;

import java.util.List;

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

    List<UserLoadContactVO> loadContact(String userId, UserContactTypeEnum typeEnum);

    UserInfoVO getContactInfo(UserTokenDTO userToken, String contactId);

    UserInfoVO getContactUserInfo(UserTokenDTO userToken, String contactId);

    void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo);

    void removeUserContact(String userId, String contactId, UserContactStatusEnum userContactStatusEnum);

    void addContactForRobot(String userId);
}
