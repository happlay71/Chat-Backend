package online.happlay.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import online.happlay.chat.enums.BeautyAccountStatusEnum;
import online.happlay.chat.entity.po.UserInfoBeauty;
import online.happlay.chat.mapper.UserInfoBeautyMapper;
import online.happlay.chat.service.IUserInfoBeautyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 靓号 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-24
 */
@Service
public class UserInfoBeautyServiceImpl extends ServiceImpl<UserInfoBeautyMapper, UserInfoBeauty> implements IUserInfoBeautyService {
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
}
