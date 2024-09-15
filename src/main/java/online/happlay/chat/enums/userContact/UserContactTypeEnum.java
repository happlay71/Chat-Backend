package online.happlay.chat.enums.userContact;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserContactTypeEnum {
    USER(0, "U", "好友"),
    GROUP(1, "G", "群");
    private Integer type;
    private String prefix;
    private String desc;

    public static UserContactTypeEnum getByName(String name) {
        try {
            if (StrUtil.isEmpty(name)) {
                return null;
            }
            return UserContactTypeEnum.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    public static UserContactTypeEnum getByPrefix(String prefix) {
        try {
            if (StrUtil.isEmpty(prefix) || prefix.trim().isEmpty()) {
                return null;
            }
            prefix = prefix.substring(0, 1);
            for (UserContactTypeEnum typeEnum : UserContactTypeEnum.values()) {
                if (typeEnum.getPrefix().equals(prefix)) {
                    return typeEnum;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
