package online.happlay.chat.entity.dto.appUpdate;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.happlay.chat.entity.query.PageQuery;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryDTO extends PageQuery implements Serializable {

    private static final long serialVersionUID = -3489031798295458898L;

    @ApiModelProperty(value = "版本号")
    private String version;

    @ApiModelProperty(value = "更新描述")
    private String updateDesc;

    @ApiModelProperty(value = "0:未发布 1:灰度发布 2:全网发布")
    private Integer status;

    @ApiModelProperty(value = "灰度uid")
    private String grayscaleUid;

    @ApiModelProperty(value = "文件类型: 0:本地文件 1:外链")
    private Integer fileType;

    @ApiModelProperty(value = "外链地址")
    private String outerLink;
}
