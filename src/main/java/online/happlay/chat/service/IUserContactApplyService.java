package online.happlay.chat.service;

import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.UserContactApply;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.page.PaginationResultVO;

/**
 * <p>
 * 联系人申请 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
public interface IUserContactApplyService extends IService<UserContactApply> {

    PaginationResultVO loadApply(UserTokenDTO userToken, Integer pageNo);

    Integer applyAdd(UserTokenDTO userToken, String contactId, String applyInfo);

    void dealWithApply(String userId, Integer applyId, Integer status);


}
