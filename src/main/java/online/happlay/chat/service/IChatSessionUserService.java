package online.happlay.chat.service;

import online.happlay.chat.entity.po.ChatSessionUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 会话用户 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
public interface IChatSessionUserService extends IService<ChatSessionUser> {

    /**
     * 根据contactId查找并返回集合
     * @param contactId
     * @return
     */
    List<ChatSessionUser> getByContactId(String contactId);

    /**
     * 根据会话用户中的联系人id修改名称
     * @param contactId
     * @param contactName
     */
    void updateNameByContactId(String contactId, String contactName);
}
