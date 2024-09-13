package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 会话用户 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "会话用户")
@RequestMapping("/chatSessionUser")
public class ChatSessionUserController extends BaseController{

}
