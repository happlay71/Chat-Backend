package online.happlay.chat.entity.dto.userBeauty;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.happlay.chat.entity.query.PageQuery;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserBeautyQueryDTO extends PageQuery implements Serializable {

    private static final long serialVersionUID = 8933665634677550307L;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "0：未使用 1：已使用")
    private Integer status;
}
