package online.happlay.chat.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import online.happlay.chat.constants.Constants;

import java.io.Serializable;

import static online.happlay.chat.constants.Constants.ROBOT_UID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDTO implements Serializable {
    private static final long serialVersionUID = 3499420748530135059L;

    @ApiModelProperty(value = "最大群组数")
    private Integer maxGroupCount = 5;
    @ApiModelProperty(value = "群组最大人数")
    private Integer maxGroupMemberCount = 500;
    @ApiModelProperty(value = "图片大小")
    private Integer maxImageSize = 2;
    @ApiModelProperty(value = "视频大小")
    private Integer maxVideoSize = 5;
    @ApiModelProperty(value = "文件大小")
    private Integer maxFileSize = 5;
    @ApiModelProperty(value = "机器人ID")
    private String robotUid = ROBOT_UID;
    @ApiModelProperty(value = "机器人昵称")
    private String robotNicName = "HapplayChat";
    @ApiModelProperty(value = "欢迎语")
    private String robotWelcome = "这里是HapplayChat机器人，欢迎使用！";
}
