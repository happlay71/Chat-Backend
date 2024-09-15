package online.happlay.chat.controller;

import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.redis.RedisUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

public class BaseController {

    @Resource
    private RedisUtils redisUtils;

    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO<>();
        vo.setStatus(STATUS_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected UserTokenDTO getUserToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        return (UserTokenDTO) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
    }
}
