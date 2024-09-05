package online.happlay.chat.service;

import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.po.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 * 用户信息 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
public interface IUserInfoService extends IService<UserInfo> {

    /**
     * 根据邮箱获取用户
     * @param email
     * @return
     */
    UserInfo getByEmail(String email);
    /**
     * 注册
     * @param email
     * @param nickName
     * @param password
     */
    void register(String email, String nickName, String password);

    /**
     * 登录
     * @param email
     * @param password
     */
    UserInfoVO login(String email, String password);

    UserInfoVO getUserInfo(UserTokenDTO userToken);

    void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;
}
