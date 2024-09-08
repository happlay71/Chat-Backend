package online.happlay.chat.redis;

import lombok.RequiredArgsConstructor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import org.springframework.stereotype.Component;

import static online.happlay.chat.constants.Constants.*;

@Component
@RequiredArgsConstructor
public class RedisComponent {

    private final RedisUtils redisUtils;

    /**
     * 获取心跳
     * @param userId
     * @return
     */
    public Long getUserHeartBeat(String userId) {
        return (Long) redisUtils.get(REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }

    /**
     * 保存token到redis
     * @param userTokenDTO
     */
    public void saveUserTokenDTO(UserTokenDTO userTokenDTO) {
        redisUtils.set(REDIS_KEY_WS_TOKEN + userTokenDTO.getToken(), userTokenDTO, REDIS_TOKEN_OUTTIME * 2);
        redisUtils.set(REDIS_KEY_WS_TOKEN_USERID + userTokenDTO.getToken(), userTokenDTO.getToken(), REDIS_TOKEN_OUTTIME * 2);
    }

    public SysSettingDTO getSysSetting() {
        SysSettingDTO sysSettingDTO = (SysSettingDTO) redisUtils.get(REDIS_KEY_SYS_SETTING);
        return sysSettingDTO == null ? new SysSettingDTO() : sysSettingDTO;
    }

    public void saveSysSetting(SysSettingDTO sysSettingDTO) {
        redisUtils.set(REDIS_KEY_SYS_SETTING, sysSettingDTO);
    }
}
