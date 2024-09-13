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
 * 聊天消息表
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_message")
@ApiModel(value="ChatMessage对象", description="聊天消息表")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "消息自增ID")
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    @ApiModelProperty(value = "会话ID")
    private String sessionId;

    @ApiModelProperty(value = "消息类型")
    private Integer messageType;

    @ApiModelProperty(value = "消息内容")
    private String messageContent;

    @ApiModelProperty(value = "发送人ID")
    private String sendUserId;

    @ApiModelProperty(value = "发送人昵称")
    private String sendUserNickName;

    @ApiModelProperty(value = "发送时间")
    private Long sendTime;

    @ApiModelProperty(value = "接收联系人ID")
    private String contactId;

    @ApiModelProperty(value = "联系人类型")
    private Integer contactType;

    @ApiModelProperty(value = "文件大小")
    private Long fileSize;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件类型")
    private Integer fileType;

    @ApiModelProperty(value = "状态: 0:正在发送 1:已发送")
    private Integer status;


}
