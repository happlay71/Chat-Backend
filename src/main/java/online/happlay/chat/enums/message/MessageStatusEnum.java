package online.happlay.chat.enums.message;

import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageStatusEnum {
    SENDING(0, "发送中"),
    SENDEND(1, "已发送");

    private Integer status;
    private String desc;

    public static MessageStatusEnum getByStatus(Integer status) {
        for (MessageStatusEnum item : MessageStatusEnum.values()) {
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
