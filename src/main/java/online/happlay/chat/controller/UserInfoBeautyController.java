package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.userBeauty.UserBeautyQueryDTO;
import online.happlay.chat.entity.po.UserInfoBeauty;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.service.IUserInfoBeautyService;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

/**
 * <p>
 * 靓号 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@RestController
@RequestMapping("/userBeauty")
@RequiredArgsConstructor
@Api(tags = "靓号管理")
public class UserInfoBeautyController extends BaseController{
    private final IUserInfoBeautyService userInfoBeautyService;

    @ApiOperation("管理员加载靓号信息")
    @GetMapping("/loadBeautyAccountList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadBeautyAccountList(@ModelAttribute UserBeautyQueryDTO userBeautyQueryDTO) {
        PaginationResultVO<UserInfoBeauty> resultVO = userInfoBeautyService.loadBeautyAccountList(userBeautyQueryDTO);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("管理员新增/修改靓号信息")
    @PostMapping("/saveBeautyAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveBeautyAccount(@ModelAttribute UserInfoBeauty userInfoBeauty) {
        if (null == userInfoBeauty.getId()) {
            // 新增
            userInfoBeautyService.saveBeautyAccount(userInfoBeauty);
        } else {
            userInfoBeautyService.updateBeautyAccount(userInfoBeauty);
        }
        return getSuccessResponseVO(null);
    }

    @ApiOperation("管理员删除靓号")
    @PostMapping("/delBeautyAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delBeautyAccount(@RequestParam("id") @NotNull Integer id) {
        userInfoBeautyService.removeById(id);
        return getSuccessResponseVO(null);
    }
}
