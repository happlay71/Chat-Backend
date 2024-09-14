package online.happlay.chat.entity.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserTokenDTO implements Serializable {
    private static final long serialVersionUID = -3244262035649152692L;

    private String token;
    private String userId;
    private String nickName;
    private Boolean admin;
}
