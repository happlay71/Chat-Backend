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
    public static final String IMAGE_SUFFIX = ".png";
    public static final String COVER_IMAGE_SUFFIX = "_cover.png";
}
