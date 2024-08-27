package online.happlay.chat.service;

import online.happlay.chat.entity.po.UserInfoBeauty;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 靓号 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
public interface IUserInfoBeautyService extends IService<UserInfoBeauty> {
    /**
     * 根据邮箱获取靓号
     * @param email
     * @return
     */
    UserInfoBeauty getByEmail(String email);

    void useBeauty(UserInfoBeauty beauty);
}
