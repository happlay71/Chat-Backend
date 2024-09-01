package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.entity.vo.UserContactApplyLoadVO;
import online.happlay.chat.entity.vo.UserContactSearchResultVO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.vo.ResponseVO;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.enums.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.service.IUserContactApplyService;
import online.happlay.chat.service.IUserContactService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

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

    @ApiOperation("查看好友申请")
    @GetMapping("/loadApply")
    @GlobalInterceptor
    public ResponseVO loadApply(HttpServletRequest request, @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo) {
        UserTokenDTO userToken = getUserToken(request);
        PaginationResultVO<UserContactApplyLoadVO> resultVO = userContactApplyService.loadApply(userToken, pageNo);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("处理好友申请")
    @PostMapping("/dealWithApply")
    @GlobalInterceptor
    public ResponseVO dealWithApply(HttpServletRequest request,
                                    @RequestParam("applyId") @NotNull Integer applyId,
                                    @RequestParam("status") @NotNull Integer status) {
        UserTokenDTO userToken = getUserToken(request);
        userContactApplyService.dealWithApply(userToken.getUserId(), applyId, status);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("处理好友申请")
    @PostMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVO loadContact(HttpServletRequest request,
                                    @RequestParam("contactType") @NotNull String contactType) {
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByName(contactType);
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserTokenDTO userToken = getUserToken(request);
        List<UserContact> contactList = userContactApplyService.loadContact(userToken.getUserId(), contactType);
        return getSuccessResponseVO(contactList);
    }
}
