package online.happlay.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.po.UserContactApply;
import online.happlay.chat.entity.vo.PaginationResultVO;
import online.happlay.chat.entity.vo.UserContactApplyLoadVO;
import online.happlay.chat.enums.*;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserContactApplyMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IUserContactApplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserContactService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * <p>
 * 联系人申请 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Service
@RequiredArgsConstructor
public class UserContactApplyServiceImpl extends ServiceImpl<UserContactApplyMapper, UserContactApply> implements IUserContactApplyService {

    private final RedisComponent redisComponent;

    @Lazy
    @Resource
    private IUserContactService userContactService;

    @Override
    public PaginationResultVO loadApply(UserTokenDTO userToken, Integer pageNo) {
        // 设置默认的分页大小
        int pageSize = PageSize.SIZE15.getSize();

        // 计算分页偏移量
        int offset = (pageNo - 1) * pageSize;

        // 执行查询，查询指定接收人的申请记录
        // 按最后申请时间倒叙排序
        List<UserContactApplyLoadVO> list = this.baseMapper.findWithContactName(userToken.getUserId(), offset, pageSize);

        // 统计总数
        int count = this.baseMapper.countByReceiveUserId(userToken.getUserId());

        // 计算总页数
        int pageTotal = (int) Math.ceil((double) count / pageSize);

        // 构建分页结果
        PaginationResultVO<UserContactApplyLoadVO> result = new PaginationResultVO<>(
                count, pageSize, pageNo, pageTotal, list);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealWithApply(String userId, Integer applyId, Integer status) {
        // 获取处理结果类型
        UserContactApply applyInfo = this.getById(applyId);
        // 判断请求是否存在或属于当前用户
        if (applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
        // 类型为空或为请求状态---处理申请的状态不能是‘请求’
        if (null == statusEnum || UserContactApplyStatusEnum.INIT == statusEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        LambdaQueryWrapper<UserContactApply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContactApply::getApplyId, applyId)
                .eq(UserContactApply::getStatus, UserContactApplyStatusEnum.INIT.getStatus());
        UserContactApply apply = this.getOne(queryWrapper);

        if (apply == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        LocalDateTime time = LocalDateTime.now();
        apply.setStatus(statusEnum.getStatus());
        apply.setLastApplyTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        this.updateById(apply);

        // TODO 拒绝可以发送WS消息给对方

        // 请求通过
        if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)) {
            // 添加联系人
            userContactService.addContact(apply.getApplyUserId(), apply.getReceiveUserId(),
                    apply.getContactId(), apply.getContactType(), apply.getApplyInfo());
            return;
        }

        // 拉黑对方
        if (UserContactApplyStatusEnum.BLACKLIST == statusEnum) {
            // 在联系人表中添加拉黑信息
            // TODO 为什么要考虑更新的情况？
            // TODO 需不需在数据库里添加双方拉黑和被拉黑情况？这里只添加了被拉黑方的情况
            UserContact userContact = new UserContact();
            userContact.setUserId(apply.getApplyUserId());
            userContact.setContactId(apply.getContactId());
            userContact.setContactType(apply.getContactType());
            userContact.setCreateTime(LocalDateTime.now());
            userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
            userContact.setLastUpdateTime(LocalDateTime.now());
            userContactService.save(userContact);
        }
    }

}
