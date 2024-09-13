package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 聊天消息表 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "聊天消息")
@RequestMapping("/chatMessage")
public class ChatMessageController extends BaseController{

}
