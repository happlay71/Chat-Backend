package online.happlay.chat.entity.vo.group;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MyGroups {
    @ApiModelProperty(value = "群ID")
    private String groupId;

    @ApiModelProperty(value = "群组名")
    private String groupName;

    @ApiModelProperty(value = "状态 1：正常 0：解散")
    private Integer status;
}
