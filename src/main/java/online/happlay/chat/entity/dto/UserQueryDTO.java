package online.happlay.chat.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.happlay.chat.entity.query.PageQuery;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryDTO extends PageQuery implements Serializable {

    private static final long serialVersionUID = -5231345205730833808L;

    @ApiModelProperty(value = "用户id")
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
}
