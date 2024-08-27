package online.happlay.chat.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserContactVO {

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "联系人ID或群组ID")
    private String contactId;

    @ApiModelProperty(value = "联系人类型 0:好友 1:群组")
    private Integer contactType;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "状态 0:非好友 1:好友 2:已删除好友 3:被好友删除 4:已拉黑好友 5:被好友拉黑")
    private Integer status;

    @ApiModelProperty(value = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    @ApiModelProperty(value = "联系人姓名")
    private String contactName;

    @ApiModelProperty(value = "联系人性别")
    private Integer sex;
}
