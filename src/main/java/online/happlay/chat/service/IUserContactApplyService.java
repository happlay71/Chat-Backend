package online.happlay.chat.service;

import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.po.UserContactApply;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.PaginationResultVO;

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

    void dealWithApply(String userId, Integer applyId, Integer status);
}
