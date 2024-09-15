package online.happlay.chat.controller;


import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.appUpdate.AppPostDTO;
import online.happlay.chat.entity.dto.appUpdate.AppQueryDTO;
import online.happlay.chat.entity.dto.appUpdate.AppSaveDTO;
import online.happlay.chat.entity.po.AppUpdate;
import online.happlay.chat.entity.vo.appUpdate.AppUpdateVO;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.enums.appUpdate.AppUpdateStatusEnum;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.service.IAppUpdateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * <p>
 * app发布 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-09-08
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "版本控制")
@RequestMapping("/appUpdate")
public class AppUpdateController extends BaseController{
    private final IAppUpdateService appUpdateService;

    @ApiOperation("加载版本列表")
    @GetMapping("/loadList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO<PaginationResultVO<AppUpdate>> loadUpdateList(
            @ModelAttribute AppQueryDTO appQueryDTO
    ) {
        // 按照创建时间倒序
        PaginationResultVO<AppUpdate> resultVO = appUpdateService.loadUpdateList(appQueryDTO);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("新增/更新发布版本")
    @PostMapping("/saveOrUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveUpdateList(
            @ModelAttribute AppSaveDTO appSaveDTO,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        // TODO 管理员应该是上传更新版本而不是将新版本下载到本地
        if (appSaveDTO.getId() == null) {
            appUpdateService.saveUpdate(appSaveDTO, file);
        } else {
            appUpdateService.changeUpdate(appSaveDTO, file);
        }
        return getSuccessResponseVO(null);
    }

    @ApiOperation("删除版本")
    @PostMapping("/delete")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delUpdate(@RequestParam("id") @NotNull Integer id) {
        AppUpdate appUpdate = appUpdateService.getById(id);
        if (!AppUpdateStatusEnum.INIT.getStatus().equals(appUpdate.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        appUpdateService.removeById(id);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("发布版本")
    @PostMapping("/post")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO postUpdate(@ModelAttribute AppPostDTO appPostDTO) {
        appUpdateService.postUpdate(appPostDTO);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("检测版本更新")
    @PostMapping("/check")
    @GlobalInterceptor
    public ResponseVO<AppUpdateVO> checkUpdate(@RequestParam(value = "appVersion", required = false) String appVersion,
                                  @RequestParam("uid") String uid) {
        if (StrUtil.isEmpty(appVersion)) {
            return getSuccessResponseVO(null);
        }
        AppUpdateVO appUpdateVO = appUpdateService.checkUpdate(appVersion, uid);
        return getSuccessResponseVO(appUpdateVO);
    }


}
