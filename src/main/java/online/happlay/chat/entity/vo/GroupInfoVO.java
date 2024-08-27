package online.happlay.chat.entity.vo;

import lombok.Data;
import online.happlay.chat.entity.po.GroupInfo;
import java.util.List;

@Data
public class GroupInfoVO {
    private GroupInfo groupInfo;
    private List<UserContactVO> userContactVOList;
}
