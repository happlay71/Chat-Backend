package online.happlay.chat.utils;

import cn.hutool.core.util.StrUtil;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static online.happlay.chat.constants.Constants.LENGTH_11;

public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String getUserId() {
        return UserContactTypeEnum.USER.getPrefix() + getRandomNumber(LENGTH_11);
    }
    public static String getGroupId() {
        return UserContactTypeEnum.GROUP.getPrefix() + getRandomNumber(LENGTH_11);
    }

    public static String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static final String encodeMd5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static final String getChatSessionIdForUser(String[] userIds) {
        Arrays.sort(userIds);
        return encodeMd5(StringUtils.join(userIds, ""));
    }

    public static final String getChatSessionIdForGroup(String groupId) {
        return encodeMd5(groupId);
    }

    /**
     * 防止HTML注入
     * @param content
     * @return
     */
    public static String cleanHtmlTag(String content) {
        if (StrUtil.isEmpty(content)) {
            return content;
        }
        // 将字符串中的所有 < 替换为 &lt;。这是为了防止 HTML 注入攻击，或者防止浏览器错误地将某些内容识别为 HTML 标签
        content = content.replace("<", "&lt;");
        // 将换行符替换为 <br> 标签
        content = content.replace("\r\n", "<br>");
        content = content.replace("\n", "<br>");
        return content;
    }

    /**
     * 获取文件名后最
     * @param fileName
     * @return
     */
    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 判断是否为数字
     * @param str
     * @return
     */
    public static boolean isNumber(String str) {
        String checkNumber = "^[0-9]+$";
        if (null == str) {
            return false;
        }
        if (!str.matches(checkNumber)) {
            return false;
        }
        return true;
    }
}