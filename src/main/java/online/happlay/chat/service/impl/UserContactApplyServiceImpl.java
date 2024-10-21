package online.happlay.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.po.UserContactApply;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.entity.vo.userContactApply.UserContactApplyLoadVO;
import online.happlay.chat.enums.*;
import online.happlay.chat.enums.group.GroupStatusEnum;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.JoinTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.enums.userContactApply.UserContactApplyStatusEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserContactApplyMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IGroupInfoService;
import online.happlay.chat.service.IUserContactApplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserContactService;
import online.happlay.chat.service.IUserInfoService;
import online.happlay.chat.websocket.netty.MessageHandler;
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

    private final MessageHandler messageHandler;

    private final RedisComponent redisComponent;

    @Lazy
    @Resource
    private IUserContactService userContactService;

    @Lazy
    @Resource
    private IGroupInfoService groupInfoService;

    @Resource
    private IUserInfoService userInfoService;

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
    @Transactional
    public Integer applyAdd(UserTokenDTO userToken, String contactId, String applyInfo) {
        // 1.获取id前缀
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 申请人
        String applyUserId = userToken.getUserId();

        // 设置默认申请信息
        applyInfo = StrUtil.isEmpty(applyInfo) ?
                String.format(Constants.APPLY_INFO_TEMPLATE, userToken.getNickName()) : applyInfo;

        Integer joinType = null;
        String receiveUserId = contactId;

        // 查询是否已添加，被拉黑无法添加
        // TODO 可能判断首次被拉黑没做？？？ P15
        UserContact userContact = userContactService.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, applyUserId)
                .eq(UserContact::getContactId, contactId));
        if (userContact != null && UserContactStatusEnum.BLACKLIST_BE.getStatus().equals(userContact.getStatus())) {
            throw new BusinessException("对方已经将你拉黑，无法添加！");
        }

        switch (typeEnum) {
            case GROUP:
                GroupInfo groupInfo = groupInfoService.getById(contactId);
                if (groupInfo == null || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())) {
                    throw new BusinessException("群聊不存在或已解散");
                }
                // 获取群主id，便于发送申请
                receiveUserId = groupInfo.getGroupOwnerId();
                joinType = groupInfo.getJoinType();
                break;
            case USER:
                UserInfo userInfo = userInfoService.getById(contactId);
                if (userInfo == null) {
                    throw new BusinessException(ResponseCodeEnum.CODE_600);
                }
                joinType = userInfo.getJoinType();
                break;
        }

        // 直接加入，不用添加到申请记录
        if (JoinTypeEnum.JOIN.getType().equals(joinType)) {
            // 添加联系人
            userContactService.addContact(applyUserId, receiveUserId, contactId, typeEnum.getType(), applyInfo);
            return joinType;
        }

        // 需要申请，数据库保存记录
        UserContactApply apply = this.getOne(new LambdaQueryWrapper<UserContactApply>()
                .eq(UserContactApply::getApplyUserId, applyUserId)
                .eq(UserContactApply::getReceiveUserId, receiveUserId)
                .eq(UserContactApply::getContactId, contactId));

        LocalDateTime time = LocalDateTime.now();

        if (apply == null) {
            // 初次申请
            UserContactApply userContactApply = new UserContactApply();
            userContactApply.setApplyUserId(applyUserId);
            userContactApply.setContactId(contactId);
            userContactApply.setContactType(typeEnum.getType());
            userContactApply.setReceiveUserId(receiveUserId);
            userContactApply.setLastApplyTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            userContactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            userContactApply.setApplyInfo(applyInfo);
            this.save(userContactApply);
        } else {
            // 更新申请
            apply.setStatus(UserContactApplyStatusEnum.REJECT.getStatus());
            apply.setLastApplyTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            apply.setApplyInfo(applyInfo);
            this.updateById(apply);
        }

        if (apply == null || !UserContactApplyStatusEnum.INIT.getStatus().equals(apply.getStatus())) {
            // 发送WS消息
            MessageSendDTO messageSendDTO = new MessageSendDTO();
            messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
            messageSendDTO.setMessageContent(applyInfo);
            messageSendDTO.setContactId(receiveUserId);
            messageHandler.sendMessage(messageSendDTO);
        }

        return joinType;
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
