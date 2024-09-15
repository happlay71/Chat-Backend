package online.happlay.chat.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.redis.RedisComponent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Api(tags = "系统设置")
public class SystemController extends BaseController {

    private final RedisComponent redisComponent;

    private final CommonConfig commonConfig;

    @ApiOperation("获取系统设置")
    @GetMapping("/getSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO<SysSettingDTO> getSysSetting() {
        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        return getSuccessResponseVO(sysSettingDTO);
    }

    @ApiOperation("修改系统设置")
    @PostMapping("/saveSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveSysSetting(@ModelAttribute SysSettingDTO sysSettingDTO,
                                     @RequestPart(value = "robotFile", required = false) MultipartFile robotFile,
                                     @RequestPart(value = "robotCover", required = false) MultipartFile robotCover) throws IOException {
        // 保存机器人头像
        if (robotFile != null) {
            String baseFolder = commonConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
            if (!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }
            String filePath = targetFileFolder.getPath() + "/" + Constants.ROBOT_UID + Constants.IMAGE_SUFFIX;
            robotFile.transferTo(new File(filePath));
            robotCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
        }

        redisComponent.saveSysSetting(sysSettingDTO);
        return getSuccessResponseVO(null);
    }


}
