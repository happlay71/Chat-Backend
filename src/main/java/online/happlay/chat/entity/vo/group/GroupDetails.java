package online.happlay.chat.entity.vo.group;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.happlay.chat.entity.query.PageQuery;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroupDetails extends PageQuery implements Serializable {

    private static final long serialVersionUID = 6758916958655694226L;

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

    @ApiModelProperty(value = "成员数")
    private Integer memberCount;

    @ApiModelProperty(value = "群主名")
    private String groupOwnerNickName;
}
