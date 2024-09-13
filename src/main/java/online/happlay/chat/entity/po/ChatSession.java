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
 * 会话信息
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_session")
@ApiModel(value="ChatSession对象", description="会话信息")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "会话ID")
    @TableId(value = "session_id", type = IdType.AUTO)
    private String sessionId;

    @ApiModelProperty(value = "最后接受的信息")
    private String lastMessage;

    @ApiModelProperty(value = "最后接受消息时间(毫秒)")
    private Long lastReceiveTime;


}
