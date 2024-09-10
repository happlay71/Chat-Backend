package online.happlay.chat.constants;

import online.happlay.chat.enums.UserContactTypeEnum;

public class Constants {

    public static final String REDIS_KEY_CHECK_CODE = "chat:checkCode:";
    public static final String REDIS_KEY_WS_USER_HEART_BEAT = "chat:ws:user:heartbeat:";
    public static final String REDIS_KEY_WS_TOKEN = "chat:ws:token:";
    public static final String REDIS_KEY_WS_TOKEN_USERID = "chat:ws:token:userid:";
    public static final String REDIS_KEY_SYS_SETTING = "chat:sys:setting:";
    public static final String ROBOT_UID = UserContactTypeEnum.USER.getPrefix() + "robot:";

    public static final Integer REDIS_CHECK_CODE_OUTTIME = 60;
    public static final Integer REDIS_TOKEN_OUTTIME = REDIS_CHECK_CODE_OUTTIME * 60 * 24;
    public static final Integer LENGTH_11 = 11;
    public static final Integer LENGTH_20 = 20;

    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";
    public static final String APP_UPDATE_FOLDER = "/app/";
    public static final String IMAGE_SUFFIX = ".png";
    public static final String COVER_IMAGE_SUFFIX = "_cover.png";
    public static final String APP_EXE_SUFFIX = ".exe";
    public static final String APP_NAME = "ChatSetup.";

    public static final String APPLY_INFO_TEMPLATE = "我是%s";

    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";

    public static final String EMAIL_ALREADY_EXISTS_MSG = "靓号邮箱已存在！";
    public static final String BEAUTY_ACCOUNT_ALREADY_EXISTS_MSG = "靓号已存在！";
    public static final String BEAUTY_ACCOUNT_EMAIL_REGISTERED_MSG = "靓号邮箱已被注册！";
    public static final String BEAUTY_ACCOUNT_REGISTERED_MSG = "靓号已被注册";
}
