package online.happlay.chat.entity.vo.appUpdate;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AppUpdateVO implements Serializable {
    private static final long serialVersionUID = 7360391488613141711L;

    @ApiModelProperty(value = "自增ID")
    private Integer id;

    @ApiModelProperty(value = "版本号")
    private String version;

    @ApiModelProperty(value = "更新描述")
    private List<String> updateDescList;

    @ApiModelProperty(value = "版本大小")
    private Long size;

    @ApiModelProperty(value = "文件类型: 0:本地文件 1:外链")
    private Integer fileType;

    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @ApiModelProperty(value = "外链地址")
    private String outerLink;

}
