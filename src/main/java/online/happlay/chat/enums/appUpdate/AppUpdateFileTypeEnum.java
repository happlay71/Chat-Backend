package online.happlay.chat.enums.appUpdate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppUpdateFileTypeEnum {
    LOCAL(0, "本地"),
    OUTER_LINK(1, "外链");

    private Integer type;
    private String description;

    public static AppUpdateFileTypeEnum getByType(Integer type) {
        for (AppUpdateFileTypeEnum at : AppUpdateFileTypeEnum.values()) {
            if (at.type.equals(type)) {
                return at;
            }
        }
        return null;
    }
}
