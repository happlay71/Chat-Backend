package online.happlay.chat.enums;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日期时间格式化枚举类
 */
@Getter
@AllArgsConstructor
public enum DateTimePatternEnum {
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    YYYY_MM_DD("yyyy-MM-dd"),
    YYYY_MM("yyyy-MM");

    private String pattern;
}
