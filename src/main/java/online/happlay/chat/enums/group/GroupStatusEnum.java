package online.happlay.chat.enums.group;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum GroupStatusEnum {
    NORMAL(1, "正常"),
    DISSOLUTION(0, "解散");

    private Integer status;
    private String desc;

    public static GroupStatusEnum getByStatus(Integer status) {
        for (GroupStatusEnum item : GroupStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }
}
