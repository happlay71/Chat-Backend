package online.happlay.chat.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息VO
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = -720784055540385571L;

    private String userId;

    private String email;

    private String nickName;

    private Integer joinType;

    private Integer sex;

    private String password;

    private String personalSignature;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;

    private String areaName;

    private String areaCode;

    private Long lastOffTime;

    private String token;

    private Boolean admin;

    private Integer contactStatus;

}
