package online.happlay.chat.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;

/**
 * <p>
 * 会话用户
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_session_user")
@ApiModel(value = "ChatSessionUser对象", description = "会话用户")
public class ChatSessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    @TableId(value = "user_id", type = IdType.AUTO)
    private String userId;

    @ApiModelProperty(value = "联系人ID")
    private String contactId;

    @ApiModelProperty(value = "会话ID")
    private String sessionId;

    @ApiModelProperty(value = "联系人名称")
    private String contactName;

    @ApiModelProperty(value = "最后接收的消息")
    @TableField(exist = false)
    private String lastMessage;

    @ApiModelProperty(value = "最后接收消息时间(毫秒)")
    @TableField(exist = false)
    private String lastReceiveTime;

    @ApiModelProperty(value = "群组人数")
    @TableField(exist = false)
    private String memberCount;

    @ApiModelProperty(value = "联系人类型")
    @TableField(exist = false)
    private Integer contactType;

    public Integer getContactType() {
        return UserContactTypeEnum.getByPrefix(contactId).getType();
    }

}
