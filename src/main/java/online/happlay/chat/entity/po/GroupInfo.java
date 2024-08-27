package online.happlay.chat.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 群
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("group_info")
@ApiModel(value="GroupInfo对象", description="群")
public class GroupInfo implements Serializable {
    private static final long serialVersionUID = 3957205260856495327L;

    @ApiModelProperty(value = "群ID")
    @TableId(value = "group_id", type = IdType.AUTO)
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
