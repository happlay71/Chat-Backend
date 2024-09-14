package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.UserContactApply;
import online.happlay.chat.entity.vo.UserContactSearchResultVO;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.po.UserContact;
import online.happlay.chat.entity.po.UserInfo;
import online.happlay.chat.entity.vo.UserInfoVO;
import online.happlay.chat.entity.vo.UserLoadContactVO;
import online.happlay.chat.enums.*;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.UserContactMapper;
import online.happlay.chat.redis.RedisComponent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private final RedisComponent redisComponent;

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
            addContact(applyUserId, receiveUserId, contactId, typeEnum.getType(), applyInfo);
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

    @Override
    public List<UserLoadContactVO> loadContact(String userId, UserContactTypeEnum typeEnum) {
        // TODO 展示自己创建的群，加入的群，联系人（只展示好友，被删除，被拉黑的（初次申请除外））

        // 查询所有符合条件的user_contact的内容
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, userId)
                .ne(UserContact::getStatus, UserContactStatusEnum.NOT_FRIEND.getStatus())
                .ne(UserContact::getStatus, UserContactStatusEnum.DEL.getStatus())
                .ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST.getStatus());

        switch (typeEnum) {
            case USER:
                // 查询所有符合条件的个人联系人
                queryWrapper.eq(UserContact::getContactType, UserContactTypeEnum.USER.getType());
                // 如果创建时间和最后更新时间相等且状态为被拉黑的情况，则不出现在查询结果里
                queryWrapper.and(qw ->
                                qw.ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST_BE.getStatus())
                                        .or()
                                        .apply("create_time <> last_update_time"))
                        .orderByDesc(UserContact::getLastUpdateTime);
                break;
            case GROUP:
                // 查询所有符合条件的群组联系人
                queryWrapper.eq(UserContact::getContactType, UserContactTypeEnum.GROUP.getType());
                System.out.println("群组" + this.list(queryWrapper));
                // 如果创建时间和最后更新时间相等且状态为被拉黑的情况，则不出现在查询结果里
                // 这里的意思是只要创建时间和最后更新时间不等或不为被拉黑的情况，则包含在查询记录中
                queryWrapper.and(qw ->
                        qw.ne(UserContact::getStatus, UserContactStatusEnum.BLACKLIST_BE.getStatus())
                                .or()
                                .apply("create_time <> last_update_time"))
                        .orderByDesc(UserContact::getLastUpdateTime);
                System.out.println("群组" + this.list(queryWrapper));
                break;
        }

        List<UserContact> userContacts = this.list(queryWrapper);

        List<UserLoadContactVO> collectList = userContacts.stream().map(userContact -> {
            UserLoadContactVO userLoadContactVO = BeanUtil.copyProperties(userContact, UserLoadContactVO.class);
            String contactName = null;
            switch (typeEnum) {
                case USER:
                    contactName = userInfoService.getById(userLoadContactVO.getContactId()).getNickName();
                    break;
                case GROUP:
                    contactName = groupInfoService.getById(userLoadContactVO.getContactId()).getGroupName();
                    break;
            }
            userLoadContactVO.setContactName(contactName);
            return userLoadContactVO;
        }).collect(Collectors.toList());

        return collectList;
    }

    @Override
    public UserInfoVO getContactInfo(UserTokenDTO userToken, String contactId) {
        UserInfo userInfo = userInfoService.getById(contactId);
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        // 初始化为非好友
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        // 在联系人中查找状态
        // 原操作为如果在联系人中存在则设置为FRIEND，修改为设置成联系人数据库中的状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userToken.getUserId())
                .eq(UserContact::getContactId, contactId));
        if (userContact != null) {
            userInfoVO.setContactStatus(userContact.getStatus());
        }

        return userInfoVO;
    }

    @Override
    public void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo) {
        // 群聊人数
        if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
            long count = this.count(
                    new LambdaQueryWrapper<UserContact>()
                            .eq(UserContact::getContactId, contactId)
                            .eq(UserContact::getStatus, UserContactStatusEnum.FRIEND.getStatus())
            );

            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupMemberCount()) {
                throw new BusinessException("成员已满员，无法加入");
            }
        }

        // 同意，双方好友记录写入数据库
        ArrayList<UserContact> contactList = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        // 申请人添加对方
        UserContact userContact = new UserContact();
        userContact.setUserId(applyUserId);
        userContact.setContactId(contactId);
        userContact.setContactType(contactType);
        userContact.setCreateTime(time);
        userContact.setLastUpdateTime(time);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        contactList.add(userContact);

        // 受邀人添加申请人，写入数据库，群组不用记录
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            userContact = new UserContact();
            userContact.setUserId(receiveUserId);
            userContact.setContactId(applyUserId);
            userContact.setContactType(contactType);
            userContact.setCreateTime(time);
            userContact.setLastUpdateTime(time);
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            contactList.add(userContact);
        }

        this.saveBatch(contactList);

        // TODO 如果是好友，接收人也添加申请人为好友 添加缓存

        // TODO 创建会话 发送消息
    }

    @Override
    @Transactional
    public void removeUserContact(String userId, String contactId, UserContactStatusEnum userContactStatusEnum) {
        // 双条件的主键约束会导致报错，报已存在相同数据
        // 查找并更新当前用户与好友的关系状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userId)
                .eq(UserContact::getContactId, contactId));
        if (userContact == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 只更新状态字段，避免更新其他字段导致唯一约束冲突！！！
        userContact.setStatus(userContactStatusEnum.getStatus());
        this.update(new LambdaUpdateWrapper<UserContact>()
                .set(UserContact::getStatus, userContactStatusEnum.getStatus())
                .eq(UserContact::getUserId, userContact.getUserId())
                .eq(UserContact::getContactId, userContact.getContactId()));

        // 查找并更新好友与当前用户的关系状态
        UserContact otherUserContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, contactId)
                .eq(UserContact::getContactId, userId));
        if (otherUserContact == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 根据当前用户的操作更新好友的状态
        if (UserContactStatusEnum.DEL == userContactStatusEnum) {
            otherUserContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
        } else if (UserContactStatusEnum.BLACKLIST == userContactStatusEnum) {
            otherUserContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
        }
        this.update(new LambdaUpdateWrapper<UserContact>()
                .set(UserContact::getStatus, otherUserContact.getStatus())
                .eq(UserContact::getUserId, otherUserContact.getUserId())
                .eq(UserContact::getContactId, otherUserContact.getContactId()));

        // TODO 从我的列表缓存中删除好友

        // TODO 从好友的列表缓存中删除我
    }

    @Override
    public UserInfoVO getContactUserInfo(UserTokenDTO userToken, String contactId) {

        // 在联系人中查找状态
        // 原操作为如果在联系人中存在则设置为FRIEND，修改为设置成联系人数据库中的状态
        UserContact userContact = this.getOne(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userToken.getUserId())
                .eq(UserContact::getContactId, contactId));
        if (null == userContact || !ArrayUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
        }, userContact.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getById(contactId);
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);

        return userInfoVO;
    }
}
