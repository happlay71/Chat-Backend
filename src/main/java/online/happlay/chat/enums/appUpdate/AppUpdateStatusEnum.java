package online.happlay.chat.enums.appUpdate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppUpdateStatusEnum {
    INIT(0, "未发布"),
    GRAYSCALE(1, "灰度发布"),
    ALL(2, "全网发布");

    private Integer status;
    private String description;

    public static AppUpdateStatusEnum getByStatus(Integer status) {
        for (AppUpdateStatusEnum at : AppUpdateStatusEnum.values()) {
            if (at.status.equals(status)) {
                return at;
            }
        }
        return null;
    }
}
