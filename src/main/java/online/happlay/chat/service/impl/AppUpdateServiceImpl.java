package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.appUpdate.AppPostDTO;
import online.happlay.chat.entity.dto.appUpdate.AppQueryDTO;
import online.happlay.chat.entity.dto.appUpdate.AppSaveDTO;
import online.happlay.chat.entity.po.AppUpdate;
import online.happlay.chat.entity.vo.AppUpdateVO;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.enums.AppUpdateFileTypeEnum;
import online.happlay.chat.enums.AppUpdateStatusEnum;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.AppUpdateMapper;
import online.happlay.chat.service.IAppUpdateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * app发布 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-08
 */
@Service
@RequiredArgsConstructor
public class AppUpdateServiceImpl extends ServiceImpl<AppUpdateMapper, AppUpdate> implements IAppUpdateService {

    private final CommonConfig commonConfig;

    @Override
    public PaginationResultVO<AppUpdate> loadUpdateList(AppQueryDTO appQueryDTO) {
        // 获取当前页码和页面大小
        Integer pageNo = appQueryDTO.getPageNo();
        Integer pageSize = appQueryDTO.getPageSize();

        // 创建分页对象
        Page<AppUpdate> page = new Page<>(pageNo, pageSize);

        // 创建查询条件
        LambdaQueryWrapper<AppUpdate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(AppUpdate::getCreateTime);

        Page<AppUpdate> newPage = this.page(page, queryWrapper);

        // 解析 updateDesc 并填充到 updateDescArray
        List<AppUpdate> appUpdateList = newPage.getRecords();
        for (AppUpdate appUpdate : appUpdateList) {
            // 自动调用 getUpdateDescArray 方法进行解析
            appUpdate.setUpdateDescArray(appUpdate.getUpdateDescArray());
        }

        // 计算总记录数
        int total = (int) this.count(queryWrapper);

        // 计算总页数 (总记录数 / 每页记录数，向上取整)
        int pages = (int) Math.ceil((double) total / pageSize);

        return new PaginationResultVO<>(
                total,
                pageSize,
                pageNo,
                pages,
                appUpdateList
        );
    }

    @Override
    public void saveUpdate(AppSaveDTO appSaveDTO, MultipartFile file) throws IOException {
        AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appSaveDTO.getFileType());
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        checkVersion(appSaveDTO);

        AppUpdate appUpdate = BeanUtil.copyProperties(appSaveDTO, AppUpdate.class);
        appUpdate.setCreateTime(LocalDateTime.now());
        appUpdate.setStatus(AppUpdateStatusEnum.INIT.getStatus());
        this.save(appUpdate);

        saveUpdateFile(file, appUpdate);
    }



    @Override
    public void changeUpdate(AppSaveDTO appSaveDTO, MultipartFile file) throws IOException {
        AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appSaveDTO.getFileType());
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        AppUpdate appUpdate = this.getById(appSaveDTO.getId());
        // 检查记录是否存在
        if (appUpdate == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 更新时不能修改发布状态和灰度uid
        AppUpdate updatedAppUpdate = new AppUpdate();
        updatedAppUpdate.setId(appUpdate.getId());
        updatedAppUpdate.setVersion(appSaveDTO.getVersion());
        updatedAppUpdate.setUpdateDesc(appSaveDTO.getUpdateDesc());
        updatedAppUpdate.setFileType(appSaveDTO.getFileType());
        updatedAppUpdate.setOuterLink(appSaveDTO.getOuterLink());
        updatedAppUpdate.setStatus(appUpdate.getStatus()); // 保持原状态
        updatedAppUpdate.setGrayscaleUid(appUpdate.getGrayscaleUid()); // 保持原灰度 UID
        this.updateById(updatedAppUpdate);

        saveUpdateFile(file, updatedAppUpdate);
    }

    @Override
    public void postUpdate(AppPostDTO appPostDTO) {
        AppUpdateStatusEnum statusEnum = AppUpdateStatusEnum.getByStatus(appPostDTO.getStatus());
        if (null == statusEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if (AppUpdateStatusEnum.GRAYSCALE == statusEnum && StrUtil.isEmpty(appPostDTO.getGrayscaleUid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if (AppUpdateStatusEnum.GRAYSCALE != statusEnum) {
            appPostDTO.setGrayscaleUid("");
        }

        AppUpdate appUpdate = this.getById(appPostDTO.getId());
        appUpdate.setStatus(appPostDTO.getStatus());
        appUpdate.setGrayscaleUid(appPostDTO.getGrayscaleUid());
        this.updateById(appUpdate);
    }

    @Override
    public AppUpdateVO checkUpdate(String appVersion, String uid) {
        AppUpdate appUpdate = this.baseMapper.checkVersion(appVersion, uid);
        if (appUpdate == null) {
            return null;
        }

        AppUpdateVO appUpdateVO = BeanUtil.copyProperties(appUpdate, AppUpdateVO.class);
        // 如果更新文件是本地，则查询并设置文件大小
        if (AppUpdateFileTypeEnum.LOCAL.getType().equals(appUpdate.getFileType())) {
            File file = new File(commonConfig.getProjectFolder() +
                    Constants.APP_UPDATE_FOLDER + appUpdate.getId() +
                    Constants.APP_EXE_SUFFIX);
            appUpdateVO.setSize(file.length());
        } else {
            appUpdateVO.setSize(0L);
        }

        // 设置更新描述
        appUpdateVO.setUpdateDescList(Arrays.asList(appUpdate.getUpdateDescArray()));
        String fileName = Constants.APP_NAME + appUpdate.getVersion() + Constants.APP_EXE_SUFFIX;
        appUpdateVO.setFileName(fileName);
        return appUpdateVO;
    }

    private void saveUpdateFile(MultipartFile file, AppUpdate appUpdate) throws IOException {
        if (file != null) {
            File folder = new File(commonConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.transferTo(new File(folder.getAbsolutePath() + "/" + appUpdate.getId() + Constants.APP_EXE_SUFFIX));
        }
    }

    /**
     * 对于未发布的版本，版本号不能低于已发布的版本，
     * 但是可以改为任意不存在于数据库里的版本
     * @param appSaveDTO
     */
    private void checkVersion(AppSaveDTO appSaveDTO) {

        if (appSaveDTO.getId() != null) {
            AppUpdate appUpdate = this.getById(appSaveDTO.getId());
            if (!AppUpdateStatusEnum.INIT.getStatus().equals(appUpdate.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        // 获取最新的已发布版本
        LambdaQueryWrapper<AppUpdate> queryWrapperPublished = new LambdaQueryWrapper<>();
        queryWrapperPublished.eq(AppUpdate::getStatus, 2) // 2表示已发布状态
                .orderByDesc(AppUpdate::getVersion)
                .last("LIMIT 1");

        AppUpdate latestPublishedUpdate = this.getOne(queryWrapperPublished, false);

        // 如果存在已发布版本，确保新版本号大于已发布的版本号
        if (latestPublishedUpdate != null) {
            String latestPublishedVersion = latestPublishedUpdate.getVersion();
            String newVersion = appSaveDTO.getVersion();

            // 添加新版本时：传入的版本号必须大于已发布的历史版本号
            if (appSaveDTO.getId() == null) {
                if (isVersionInvalid(newVersion, latestPublishedVersion)) {
                    throw new BusinessException("当前版本必须大于已发布的历史版本！");
                }
            }

            // 更新现有版本时：确保更新后的版本号仍然有效
            if (appSaveDTO.getId() != null) {
                if (isVersionInvalid(newVersion, latestPublishedVersion) && !newVersion.equals(latestPublishedVersion)) {
                    throw new BusinessException("当前版本必须大于已发布的历史版本！");
                }
            }
        }

        // 检查新版本号是否已存在（排除当前记录）
        LambdaQueryWrapper<AppUpdate> queryWrapperUnique = new LambdaQueryWrapper<>();
        queryWrapperUnique.eq(AppUpdate::getVersion, appSaveDTO.getVersion());

        // 如果是更新操作，排除当前记录
        if (appSaveDTO.getId() != null) {
            queryWrapperUnique.ne(AppUpdate::getId, appSaveDTO.getId());
        }

        if (this.getOne(queryWrapperUnique) != null) {
            throw new BusinessException("版本号已存在！");
        }
    }

    /**
     * 比较版本号
     * @param newVersion
     * @param latestVersion
     * @return
     */
    private boolean isVersionInvalid(String newVersion, String latestVersion) {
        // 使用合适的版本号比较库，此处 Maven Artifact Version Comparator
        DefaultArtifactVersion newVer = new DefaultArtifactVersion(newVersion);
        DefaultArtifactVersion latestVer = new DefaultArtifactVersion(latestVersion);
        return newVer.compareTo(latestVer) <= 0;
    }
}
