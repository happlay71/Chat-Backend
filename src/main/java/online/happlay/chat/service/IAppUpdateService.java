package online.happlay.chat.service;

import online.happlay.chat.entity.dto.appUpdate.AppPostDTO;
import online.happlay.chat.entity.dto.appUpdate.AppQueryDTO;
import online.happlay.chat.entity.dto.appUpdate.AppSaveDTO;
import online.happlay.chat.entity.po.AppUpdate;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.appUpdate.AppUpdateVO;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 * app发布 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-09-08
 */
public interface IAppUpdateService extends IService<AppUpdate> {

    PaginationResultVO<AppUpdate> loadUpdateList(AppQueryDTO appQueryDTO);


    void saveUpdate(AppSaveDTO appSaveDTO, MultipartFile file) throws IOException;

    void changeUpdate(AppSaveDTO appSaveDTO, MultipartFile file) throws IOException;

    void postUpdate(AppPostDTO appPostDTO);

    AppUpdateVO checkUpdate(String appVersion, String uid);
}
