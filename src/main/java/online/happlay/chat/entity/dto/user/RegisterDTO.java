package online.happlay.chat.entity.dto.user;

import lombok.Data;
import online.happlay.chat.constants.Constants;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegisterDTO {
    @NotBlank
    private String checkCodeKey;

    @Email
    @NotBlank
    private String email;

    @NotBlank
//    @Pattern(regexp = Constants.REGEX_PASSWORD)
    private String password;

    @NotBlank
    private String nickName;

    @NotBlank
    private String checkCode;
}
