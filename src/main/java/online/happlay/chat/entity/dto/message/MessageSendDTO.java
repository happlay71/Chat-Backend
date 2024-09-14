package online.happlay.chat.entity.dto.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import jodd.util.StringUtil;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendDTO<T> implements Serializable {
    private static final long serialVersionUID = -6082849291221569719L;

    @ApiModelProperty(value = "消息ID")
    private Long messageId;

    @ApiModelProperty(value = "会话ID")
    private String sessionId;

    @ApiModelProperty(value = "消息类型")
    private Integer messageType;

    @ApiModelProperty(value = "消息内容")
    private String messageContent;

    @ApiModelProperty(value = "最后的消息")
    private String lastMessage;

    @ApiModelProperty(value = "发送人ID")
    private String sendUserId;

    @ApiModelProperty(value = "发送人昵称")
    private String sendUserNickName;

    @ApiModelProperty(value = "发送时间")
    private Long sendTime;

    @ApiModelProperty(value = "接收联系人ID")
    private String contactId;

    @ApiModelProperty(value = "接收联系人姓名")
    private String contactName;

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

    @ApiModelProperty(value = "群员数")
    private Integer memberCount;

    @ApiModelProperty(value = "扩展信息")
    private T extendData;

    public String getLastMessage() {
        if (StringUtil.isEmpty(lastMessage)) {
            return messageContent;
        }
        return lastMessage;
    }
}
