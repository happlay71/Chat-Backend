package online.happlay.chat.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.ZoneOffset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户信息
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_info")
@ApiModel(value="UserInfo对象", description="用户信息")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 4713221936019710048L;

    @ApiModelProperty(value = "用户id")
    @TableId(value = "user_id", type = IdType.AUTO)
    private String userId;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "昵称")
    private String nickName;

    @ApiModelProperty(value = "0：直接加入 1：同意后加好友")
    private Integer joinType;

    @ApiModelProperty(value = "性别 0：女 1：男")
    private Integer sex;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "个性签名")
    private String personalSignature;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "地区")
    private String areaName;

    @ApiModelProperty(value = "地区编号")
    private String areaCode;

    @ApiModelProperty(value = "最后离开时间")
    private Long lastOffTime;

    @ApiModelProperty(value = "最后在线时间")
    @TableField(exist = false)
    private Integer onlineType;

    public Integer getOnlineType() {
        if (lastLoginTime != null && lastLoginTime.toEpochSecond(ZoneOffset.UTC) > lastOffTime) {
            return 1;
        } else {
            return 0;
        }
    }
}
