package online.happlay.chat.entity.vo.userContactApply;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import online.happlay.chat.enums.userContactApply.UserContactApplyStatusEnum;

@Data
public class UserContactApplyLoadVO {

    @ApiModelProperty(value = "自增ID")
    private Integer applyId;

    @ApiModelProperty(value = "申请人ID")
    private String applyUserId;

    @ApiModelProperty(value = "接收人ID")
    private String receiveUserId;

    @ApiModelProperty(value = "联系人类型 0:好友 1:群组")
    private Integer contactType;

    @ApiModelProperty(value = "联系人ID")
    private String contactId;

    @ApiModelProperty(value = "最后申请时间")
    private Long lastApplyTime;

    @ApiModelProperty(value = "状态 0:待处理 1:已同意 2:已拒绝 3:已拉黑")
    private Integer status;

    @ApiModelProperty(value = "申请信息")
    private String applyInfo;

    @ApiModelProperty(value = "用户或群组名称")
    private String contactName;

    @ApiModelProperty(value = "状态名称")
    private String statusName;

    public String getStatusName() {
        UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
        return statusEnum == null ? null : statusEnum.getDesc();
    }
}