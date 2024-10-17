package online.happlay.chat.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.user.LoginDTO;
import online.happlay.chat.entity.dto.user.RegisterDTO;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.entity.vo.user.UserInfoVO;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.redis.RedisUtils;
import online.happlay.chat.service.IUserInfoService;
import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.utils.StringTools;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static online.happlay.chat.constants.Constants.REDIS_CHECK_CODE_OUTTIME;
import static online.happlay.chat.constants.Constants.REDIS_KEY_CHECK_CODE;

@Validated
@RestController("accountController")
@RequiredArgsConstructor
@RequestMapping("account")
@Api(tags = "登录验证")
public class AccountController extends BaseController {


    private final RedisUtils redisUtils;

    private final IUserInfoService userInfoService;

    private final RedisComponent redisComponent;

    @ApiOperation("发送验证码")
    @GetMapping("/checkCode")
    public ResponseVO checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();
        // 定义验证码的key
        String checkCodeKey = UUID.randomUUID().toString();
        String checkCodeBase64 = captcha.toBase64();

        // 存入redis--过期时间设置10分钟
        redisUtils.set(REDIS_KEY_CHECK_CODE + checkCodeKey, code, REDIS_CHECK_CODE_OUTTIME * 10);

        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVO(result);
    }

    @ApiOperation("注册")
    @PostMapping("/register")
    public ResponseVO register(@Validated @ModelAttribute RegisterDTO registerDTO) {
        String checkCode = registerDTO.getCheckCode();
        String checkCodeKey = registerDTO.getCheckCodeKey();
        String key = REDIS_KEY_CHECK_CODE + checkCodeKey;
        try {
            if (!checkCode.equalsIgnoreCase(
                    StrUtil.toString(redisUtils.get(key))
            )) {
                throw new BusinessException("图片验证码不正确");
            }
            // TODO 增加邮箱验证，注册时的验证码通过邮箱发给用户，然后校验
            userInfoService.register(registerDTO.getEmail(), registerDTO.getNickName(), registerDTO.getPassword());
            return getSuccessResponseVO(null);
        } finally {
            redisUtils.del(key);
        }
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    public ResponseVO register(@Validated @ModelAttribute LoginDTO loginDTO) {
        String checkCode = loginDTO.getCheckCode();
        String checkCodeKey = loginDTO.getCheckCodeKey();
        String key = REDIS_KEY_CHECK_CODE + checkCodeKey;
        try {
            if (!checkCode.equalsIgnoreCase(
                    StrUtil.toString(redisUtils.get(key))
            )) {
                throw new BusinessException("图片验证码不正确");
            }

            UserInfoVO login = userInfoService.login(loginDTO.getEmail(), loginDTO.getPassword());
            return getSuccessResponseVO(login);
        } finally {
            redisUtils.del(key);
        }
    }

    @GlobalInterceptor
    @ApiOperation("获取管理员控制页面")
    @GetMapping("/getSysSetting")
    public ResponseVO getSysSetting() {
        return getSuccessResponseVO(redisComponent.getSysSetting());
    }
}
