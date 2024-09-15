package online.happlay.chat.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.happlay.chat.entity.po.ChatMessage;
import online.happlay.chat.entity.po.ChatSessionUser;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsInitDataVO {
    private List<ChatSessionUser> chatSessionList;
    private List<ChatMessage> chatMessageList;
    // 申请条数
    private Integer applyCount;
}
