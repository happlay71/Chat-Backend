package online.happlay.chat.entity.dto.user;

import lombok.Data;
import online.happlay.chat.constants.Constants;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginDTO {
    @NotBlank
    private String checkCodeKey;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String checkCode;
}
