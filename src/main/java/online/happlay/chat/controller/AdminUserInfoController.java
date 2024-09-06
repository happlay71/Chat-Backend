package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.UserQueryDTO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.entity.vo.ResponseVO;
import online.happlay.chat.entity.vo.UserInfoVO;
import online.happlay.chat.service.IUserInfoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Api(tags = "管理员面板")
public class AdminUserInfoController extends BaseController {

    private final IUserInfoService userInfoService;


    @ApiOperation("获取所有用户信息")
    @GetMapping("/loadUser")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadUser(@ModelAttribute UserQueryDTO userQueryDTO) {
        // 按照创建时间倒序
        PaginationResultVO resultVO = userInfoService.loadUser(userQueryDTO);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("更新用户状态")
    @GetMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO updateUserStatus(@RequestParam("userId") @NotEmpty String userId,
                                       @RequestParam("status") @NotNull Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("强制用户下线")
    @GetMapping("/forceOffLine")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO forceOffLine(@RequestParam("userId") @NotEmpty String userId) {
        userInfoService.forceOffLine(userId);
        return getSuccessResponseVO(null);
    }

}
