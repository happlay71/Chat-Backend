package online.happlay.chat.aop;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.redis.RedisUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import static online.happlay.chat.constants.Constants.REDIS_KEY_WS_TOKEN;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GlobalOperationAspect {
    private final RedisUtils redisUtils;

    @Before("@annotation(online.happlay.chat.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) {
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (interceptor == null) {
                return;
            }
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
        } catch (BusinessException e) {
            log.error("全局拦截异常", e);
            throw e;
        } catch (Throwable e) {
            log.error("全局拦截异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    private void checkLogin(Boolean checkAdmin) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();;
        String token = request.getHeader("token");;
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        UserTokenDTO userTokenDTO = (UserTokenDTO) redisUtils.get(REDIS_KEY_WS_TOKEN + token);
        if (userTokenDTO == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (checkAdmin && !userTokenDTO.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }
}
