package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

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
@Api(tags = "用户信息")
public class UserInfoController extends BaseController{

}
