package online.happlay.chat.entity.vo.group;

import lombok.Data;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.vo.userContact.UserContactVO;

import java.util.List;

@Data
public class GroupInfoVO {
    private GroupInfo groupInfo;
    private List<UserContactVO> userContactVOList;
}
