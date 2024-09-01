package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.po.UserContactApply;
import online.happlay.chat.entity.vo.UserContactSearchResultVO;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.enums.*;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserContactMapper;
import online.happlay.chat.service.IGroupInfoService;
import online.happlay.chat.service.IUserContactApplyService;
import online.happlay.chat.service.IUserContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IUserInfoService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * <p>
 * 联系人 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@Service
@RequiredArgsConstructor
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact> implements IUserContactService {

    private final IUserInfoService userInfoService;

    private final IUserContactApplyService userContactApplyService;

    @Resource
    @Lazy
    private IGroupInfoService groupInfoService;

    @Override
    public UserContactSearchResultVO searchContact(String userId, String contactId) {
        // 1.获取id前缀
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (typeEnum == null) {
            return null;
        }
        UserContactSearchResultVO resultDto = new UserContactSearchResultVO();
        // 2.判断是人还是群组
        switch (typeEnum) {
            case USER:
                UserInfo userInfo = userInfoService.getById(contactId);
                if (userInfo == null) {
                    return null;
                }
                resultDto = BeanUtil.copyProperties(userInfo, UserContactSearchResultVO.class);
                break;
            case GROUP:
                GroupInfo groupInfo = groupInfoService.getById(contactId);
                if (groupInfo == null) {
                    return null;
                }
                resultDto.setNickName(groupInfo.getGroupName());
                break;
        }

        // 设置通用属性
        resultDto.setContactId(contactId);
        resultDto.setContactType(typeEnum.toString());

        // 判断是否是自己
        if (userId.equals(contactId)) {
            resultDto.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            return resultDto;
        }

        // 查询是否是好友
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, userId).eq(UserContact::getContactId, contactId);
        UserContact userContact = this.getOne(queryWrapper);
        resultDto.setStatus(userContact == null ? null : userContact.getStatus());
        return resultDto;
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
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
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
            userContactApplyService.addContact(applyUserId, receiveUserId, contactId, typeEnum.getType(), applyInfo);
            return joinType;
        }

        // 需要申请，数据库保存记录
        UserContactApply apply = userContactApplyService.getOne(new LambdaQueryWrapper<UserContactApply>()
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
            userContactApplyService.save(userContactApply);
        } else {
            // 更新申请
            apply.setStatus(UserContactApplyStatusEnum.REJECT.getStatus());
            apply.setLastApplyTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            apply.setApplyInfo(applyInfo);
            userContactApplyService.updateById(apply);
        }

        if (apply == null || !UserContactApplyStatusEnum.INIT.getStatus().equals(apply.getStatus())) {
            // TODO 发送WS消息
        }

        return joinType;
    }
}
