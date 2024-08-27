package online.happlay.chat.service.impl;

import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.mapper.UserContactMapper;
import online.happlay.chat.service.IUserContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 联系人 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Service
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact> implements IUserContactService {

}
