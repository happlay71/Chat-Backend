package online.happlay.chat.redis;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import org.springframework.stereotype.Component;

import java.util.List;

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
     * 保存用户心跳
     * @param userId
     */
    public void saveHeartBeat(String userId) {
        redisUtils.set(REDIS_KEY_WS_USER_HEART_BEAT + userId, System.currentTimeMillis(), REDIS_KEY_EXPIRES_HEART_BEAT);
    }

    /**
     * 移除用户心跳
     * @param userId
     */
    public void removeUserHeartBeat(String userId) {
        redisUtils.del(REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }

    /**
     * 保存token到redis
     * @param userTokenDTO
     */
    public void saveUserTokenDTO(UserTokenDTO userTokenDTO) {
        redisUtils.set(REDIS_KEY_WS_TOKEN + userTokenDTO.getToken(), userTokenDTO, REDIS_TOKEN_OUTTIME * 2);
        redisUtils.set(REDIS_KEY_WS_TOKEN_USERID + userTokenDTO.getUserId(), userTokenDTO.getToken(), REDIS_TOKEN_OUTTIME * 2);
    }

    /**
     * 根据token获取用户信息
     * @param token
     * @return
     */
    public UserTokenDTO getUserTokenDTO(String token) {
        UserTokenDTO userTokenDTO = (UserTokenDTO) redisUtils.get(REDIS_KEY_WS_TOKEN + token);
        return userTokenDTO;
    }

    /**
     * 获取系统设置信息
     * @return
     */
    public SysSettingDTO getSysSetting() {
        SysSettingDTO sysSettingDTO = (SysSettingDTO) redisUtils.get(REDIS_KEY_SYS_SETTING);
        return sysSettingDTO == null ? new SysSettingDTO() : sysSettingDTO;
    }

    /**
     * 保存系统信息
     * @param sysSettingDTO
     */
    public void saveSysSetting(SysSettingDTO sysSettingDTO) {
        redisUtils.set(REDIS_KEY_SYS_SETTING, sysSettingDTO);
    }

    /**
     * 清空联系人
     * @param userId
     */
    public void cleanUserContact(String userId) {
        redisUtils.del(REDIS_KEY_USER_CONTACT + userId);
    }

    /**
     * 获取列表中的联系人
     * @param userId
     * @return
     */
    public List<String> getUserContactList(String userId) {
        return redisUtils.lGet(REDIS_KEY_USER_CONTACT + userId, 0, -1);
    }

    /**
     * 批量添加联系人
     * @param userId
     * @param contactIdList
     */
    public void addUserContactBatch(String userId, List<String> contactIdList) {
        redisUtils.lSet(REDIS_KEY_USER_CONTACT + userId, contactIdList, REDIS_TOKEN_OUTTIME);
    }

    /**
     * 添加联系人
     * @param userId
     * @param contactId
     */
    public void addUserContact(String userId, String contactId) {
        List<String> userContactList = getUserContactList(userId);
        if (userContactList.contains(contactId)) {
            return;
        }
        redisUtils.lSet(REDIS_KEY_USER_CONTACT + userId, contactId, REDIS_TOKEN_OUTTIME);
    }

    /**
     * 根据用户id清空对应缓存
     * @param userId
     */
    public void cleanUserTokenById(String userId) {
        String token = (String) redisUtils.get(REDIS_KEY_WS_TOKEN_USERID + userId);
        if (StrUtil.isEmpty(token)) {
            return;
        }
        redisUtils.del(REDIS_KEY_WS_TOKEN + token);
    }
}
