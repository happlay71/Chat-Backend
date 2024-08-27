package online.happlay.chat.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 联系人申请
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_contact_apply")
@ApiModel(value="UserContactApply对象", description="联系人申请")
public class UserContactApply implements Serializable {

    private static final long serialVersionUID = 2308172027836314836L;

    @ApiModelProperty(value = "自增ID")
    @TableId(value = "apply_id", type = IdType.AUTO)
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


}
