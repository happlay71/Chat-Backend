package online.happlay.chat.entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SaveGroupDTO {
    private String groupId;
    @NotEmpty
    private String groupName;
    private String groupNotice;
    @NotNull
    private Integer joinType;
//    private MultipartFile avatarFile;
//    private MultipartFile avatarCover;
}
