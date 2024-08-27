package online.happlay.chat.entity.page;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
public class GroupPageQuery{
    @ApiModelProperty(value = "群ID")
    private String groupId;

    @ApiModelProperty(value = "群组名")
    private String groupName;

    @ApiModelProperty(value = "群主ID")
    private String groupOwnerId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "群公告")
    private String groupNotice;

    @ApiModelProperty(value = "0：直接加入 1：管理员同意后加入")
    private Integer joinType;

    @ApiModelProperty(value = "状态 1：正常 0：解散")
    private Integer status;
}
