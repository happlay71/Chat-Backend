package online.happlay.chat.entity.vo;

import lombok.Data;
import online.happlay.chat.enums.UserContactStatusEnum;

@Data
public class UserContactSearchResultVO {
    private String contactId;
    private String contactType;
    private String nickName;
    private Integer status;
    private String statusNames;
    private Integer sex;
    private String areaName;

    public String getStatusName() {
        UserContactStatusEnum statusEnum = UserContactStatusEnum.getByStatus(status);
        return statusNames == null ? null : statusEnum.getDesc();
    }
}
