package online.happlay.chat.entity.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppPostDTO {

    @ApiModelProperty(value = "自增ID", required = false)
    private Integer id;

    @ApiModelProperty(value = "0:未发布 1:灰度发布 2:全网发布")
    private Integer status;

    @ApiModelProperty(value = "灰度uid")
    private String grayscaleUid;
}
