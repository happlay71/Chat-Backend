package online.happlay.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.entity.dto.userBeauty.UserBeautyQueryDTO;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.enums.BeautyAccountStatusEnum;
import online.happlay.chat.entity.po.UserInfoBeauty;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserInfoBeautyMapper;
import online.happlay.chat.service.IUserInfoBeautyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserInfoService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static online.happlay.chat.constants.Constants.*;

/**
 * <p>
 * 靓号 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@Service
@RequiredArgsConstructor
public class UserInfoBeautyServiceImpl extends ServiceImpl<UserInfoBeautyMapper, UserInfoBeauty> implements IUserInfoBeautyService {

    @Lazy
    @Resource
    private IUserInfoService userInfoService;

    @Override
    public UserInfoBeauty getByEmail(String email) {
        LambdaQueryWrapper<UserInfoBeauty> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfoBeauty::getEmail, email);

        return this.getOne(queryWrapper);
    }

    @Override
    public void useBeauty(UserInfoBeauty beauty) {
        beauty.setStatus(BeautyAccountStatusEnum.USED.getStatus());
        this.updateById(beauty);
    }

    @Override
    public PaginationResultVO<UserInfoBeauty> loadBeautyAccountList(UserBeautyQueryDTO userBeautyQueryDTO) {
        // 获取当前页码和每页大小
        Integer pageNo = userBeautyQueryDTO.getPageNo();
        Integer pageSize = userBeautyQueryDTO.getPageSize();

        // 创建分页对象
        Page<UserInfoBeauty> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<UserInfoBeauty> queryWrapper = new LambdaQueryWrapper<>();

        if (userBeautyQueryDTO.getUserId() != null && !userBeautyQueryDTO.getUserId().isEmpty()) {
            queryWrapper.like(UserInfoBeauty::getUserId, userBeautyQueryDTO.getUserId());
        }

        if (userBeautyQueryDTO.getEmail() != null && !userBeautyQueryDTO.getEmail().isEmpty()) {
            queryWrapper.like(UserInfoBeauty::getEmail, userBeautyQueryDTO.getEmail());
        }

        // 计算总记录数
        int total = (int) this.count(queryWrapper);

        // 计算总页数 (总记录数 / 每页记录数，向上取整)
        int pages = (int) Math.ceil((double) total / pageSize);

        queryWrapper.orderByDesc(UserInfoBeauty::getId);

        Page<UserInfoBeauty> newPage = this.page(page, queryWrapper);
        return new PaginationResultVO<UserInfoBeauty>(
                total,
                pageSize,
                pageNo,
                pages,
                newPage.getRecords()
        );
    }

    /**
     * 新增靓号信息
     * @param userInfoBeauty
     */
    @Override
    public void saveBeautyAccount(UserInfoBeauty userInfoBeauty) {
        validateBeautyAccount(userInfoBeauty, null);

        userInfoBeauty.setStatus(BeautyAccountStatusEnum.NO_USE.getStatus());
        this.save(userInfoBeauty);
    }

    /**
     * 更新靓号信息
     * @param userInfoBeauty
     */
    @Override
    public void updateBeautyAccount(UserInfoBeauty userInfoBeauty) {
        // 检查是否存在该靓号
        UserInfoBeauty existingBeauty = this.getById(userInfoBeauty.getId());

        if (existingBeauty == null) {
            throw new BusinessException("靓号不存在！");
        }

        // 判断靓号是否已被使用
        if (BeautyAccountStatusEnum.USED.getStatus().equals(existingBeauty.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 校验靓号邮箱和靓号ID是否已被其他记录使用
        validateBeautyAccount(userInfoBeauty, userInfoBeauty.getId());

        this.updateById(userInfoBeauty);
    }

    /**
     * 校验靓号信息，确保邮箱和用户ID唯一
     * @param userInfoBeauty 靓号信息
     * @param excludeId 需要排除的靓号ID（用于更新操作）
     */
    private void validateBeautyAccount(UserInfoBeauty userInfoBeauty, Integer excludeId) {
        LambdaQueryWrapper<UserInfoBeauty> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .ne(excludeId != null, UserInfoBeauty::getId, excludeId) // 排除当前记录（仅用于更新）
                .and(wrapper -> wrapper
                        .eq(UserInfoBeauty::getEmail, userInfoBeauty.getEmail())
                        .or()
                        .eq(UserInfoBeauty::getUserId, userInfoBeauty.getUserId())
                );

        UserInfoBeauty existingBeauty = this.getOne(queryWrapper);

        if (existingBeauty != null) {
            if (existingBeauty.getEmail().equals(userInfoBeauty.getEmail())) {
                throw new BusinessException(EMAIL_ALREADY_EXISTS_MSG);
            }
            if (existingBeauty.getUserId().equals(userInfoBeauty.getUserId())) {
                throw new BusinessException(BEAUTY_ACCOUNT_ALREADY_EXISTS_MSG);
            }
        }

        validateBeautyAccountWithUser(userInfoBeauty);
    }

    /**
     * 校验靓号邮箱和用户ID在用户表中是否已注册
     * @param userInfoBeauty 靓号信息
     */
    private void validateBeautyAccountWithUser(UserInfoBeauty userInfoBeauty) {
        LambdaQueryWrapper<UserInfo> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper
                .eq(UserInfo::getEmail, userInfoBeauty.getEmail())
                .or()
                .eq(UserInfo::getUserId, userInfoBeauty.getUserId());

        UserInfo userInfo = userInfoService.getOne(userQueryWrapper);

        if (userInfo != null) {
            if (userInfo.getEmail().equals(userInfoBeauty.getEmail())) {
                throw new BusinessException(BEAUTY_ACCOUNT_EMAIL_REGISTERED_MSG);
            }
            if (userInfo.getUserId().equals(userInfoBeauty.getUserId())) {
                throw new BusinessException(BEAUTY_ACCOUNT_REGISTERED_MSG);
            }
        }
    }


}
