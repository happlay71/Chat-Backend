package online.happlay.chat.enums.group;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupOpTypeEnum {
    ZERO(0, "踢出群组"),
    ONE(1, "拉入群组");

    private Integer opType;
    private String desc;

    public static GroupOpTypeEnum getByOpType(Integer opType) {
        for (GroupOpTypeEnum item : GroupOpTypeEnum.values()) {
            if (item.getOpType().equals(opType)) {
                return item;
            }
        }
        return null;
    }
}
