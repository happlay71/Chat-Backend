package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.ResponseVO;
import online.happlay.chat.entity.vo.UserInfoVO;
import online.happlay.chat.service.IUserInfoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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
@RequestMapping("/userInfo")
@RequiredArgsConstructor
@Api(tags = "用户信息")
public class UserInfoController extends BaseController {

    private final IUserInfoService userInfoService;


    @ApiOperation("获取当前用户信息")
    @GetMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpServletRequest request) {
        UserTokenDTO userToken = getUserToken(request);
        UserInfoVO userInfoVO = userInfoService.getUserInfo(userToken);
        return getSuccessResponseVO(userInfoVO);
    }

    @ApiOperation("保存当前用户信息")
    @PostMapping("/saveUserInfo")
    @GlobalInterceptor
    public ResponseVO saveUserInfo(HttpServletRequest request,
                                   @ModelAttribute UserInfo userInfo,
                                   @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
                                   @RequestPart(value = "avatarCover", required = false) MultipartFile avatarCover
    ) throws IOException {
        UserTokenDTO userToken = getUserToken(request);
        // 将隐私信息设置成null
        userInfo.setUserId(userToken.getUserId());
        userInfo.setPassword(null);
        userInfo.setStatus(null);
        userInfo.setCreateTime(null);
        userInfo.setLastLoginTime(null);
        userInfoService.updateUserInfo(userInfo, avatarFile, avatarCover);
        return getUserInfo(request);
    }

    @ApiOperation("修改当前用户的密码")
    @PostMapping("/updatePassword")
    @GlobalInterceptor
    public ResponseVO updatePassword(HttpServletRequest request,
                                     @RequestParam("password") @Pattern(regexp = Constants.REGEX_PASSWORD) String password
    ) {
        UserTokenDTO userToken = getUserToken(request);
        userInfoService.updatePassword(userToken, password);
        // TODO 强制退出，重新登录
        return getSuccessResponseVO(null);
    }

    @ApiOperation("登出当前用户")
    @GetMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletRequest request) {
        UserTokenDTO userToken = getUserToken(request);
        // TODO 退出登录，关闭WS连接
        return getSuccessResponseVO(null);
    }
}
