package online.happlay.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BeautyAccountStatusEnum {
    NO_USE(0, "未使用"),
    USED(1, "已使用");

    private Integer status;
    private String desc;

    public static BeautyAccountStatusEnum getByStatus(Integer status) {
        for (BeautyAccountStatusEnum item : BeautyAccountStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
