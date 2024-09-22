package online.happlay.chat.entity.dto.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ChatSendMessageDTO {

    @ApiModelProperty(value = "接收联系人ID")
    @NotEmpty
    private String contactId;

    @ApiModelProperty(value = "消息内容")
    @NotEmpty
    @Max(500)
    private String messageContent;

    @ApiModelProperty(value = "消息类型")
    @NotNull
    private Integer messageType;

    @ApiModelProperty(value = "文件大小")
    private Long fileSize;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件类型")
    private Integer fileType;

}
