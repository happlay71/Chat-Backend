package online.happlay.chat.entity.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AppSaveDTO implements Serializable {
    private static final long serialVersionUID = 1427440384409937932L;
    @ApiModelProperty(value = "自增ID", required = false)
    private Integer id;

    @ApiModelProperty(value = "版本号", required = true)
    @NotEmpty(message = "版本号不能为空")
    private String version;

    @ApiModelProperty(value = "更新描述", required = true)
    @NotEmpty(message = "更新描述不能为空")
    private String updateDesc;

    @ApiModelProperty(value = "文件类型: 0:本地文件 1:外链", required = true)
    @NotNull(message = "文件类型不能为空")
    private Integer fileType;

    @ApiModelProperty(value = "外链地址", required = false)
    private String outerLink;
}
