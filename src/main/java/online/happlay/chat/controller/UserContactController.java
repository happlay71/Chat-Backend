package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.entity.vo.UserContactSearchResultVO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.vo.ResponseVO;
import online.happlay.chat.service.IUserContactApplyService;
import online.happlay.chat.service.IUserContactService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;

/**
 * <p>
 * 联系人 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
@Api(tags = "联系人")
public class UserContactController extends BaseController{

    private final IUserContactService userContactService;

    private final IUserContactApplyService userContactApplyService;

    @ApiOperation("搜索联系人")
    @GetMapping("/search")
    @GlobalInterceptor
    public ResponseVO search(HttpServletRequest request, @RequestParam("contactId") @NotEmpty String contactId) {
        UserTokenDTO userToken = getUserToken(request);
        UserContactSearchResultVO resultDto = userContactService.searchContact(userToken.getUserId(), contactId);
        return getSuccessResponseVO(resultDto);
    }

    @ApiOperation("添加联系人")
    @PostMapping("/applyAdd")
    @GlobalInterceptor
    public ResponseVO applyAdd(HttpServletRequest request,
                               @RequestParam("contactId") @NotEmpty String contactId,
                               @RequestParam("applyInfo") String applyInfo
    ) {
        UserTokenDTO userToken = getUserToken(request);
        Integer joinType = userContactService.applyAdd(userToken, contactId, applyInfo);
        return getSuccessResponseVO(joinType);
    }

    @ApiOperation("好友申请")
    @PostMapping("/loadApply")
    @GlobalInterceptor
    public ResponseVO loadApply(HttpServletRequest request, @RequestParam("pageNo") Integer pageNo) {
        UserTokenDTO userToken = getUserToken(request);
        PaginationResultVO resultVO = userContactApplyService.loadApply(userToken, pageNo);
        return getSuccessResponseVO(resultVO);
    }
}
