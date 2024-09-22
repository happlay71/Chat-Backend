package online.happlay.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.SysSettingDTO;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.ChatMessage;
import online.happlay.chat.entity.po.ChatSession;
import online.happlay.chat.enums.DateTimePatternEnum;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.enums.message.MessageStatusEnum;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.enums.userContact.UserContactStatusEnum;
import online.happlay.chat.enums.userContact.UserContactTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.mapper.ChatMessageMapper;
import online.happlay.chat.redis.RedisComponent;
import online.happlay.chat.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.happlay.chat.service.IChatSessionService;
import online.happlay.chat.service.IUserContactService;
import online.happlay.chat.utils.StringTools;
import online.happlay.chat.websocket.netty.MessageHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static online.happlay.chat.constants.Constants.*;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author happlay
 * @since 2024-09-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

    @Lazy
    @Resource
    private IUserContactService userContactService;

    private final IChatSessionService chatSessionService;

    private final CommonConfig commonConfig;

    private final RedisComponent redisComponent;

    private final MessageHandler messageHandler;

    @Override
    public MessageSendDTO saveMessage(ChatMessage chatMessage, UserTokenDTO userToken) {
        // 判断是否为机器人
        if (!ROBOT_UID.equals(userToken.getUserId())) {
            List<String> userContactList = redisComponent.getUserContactList(userToken.getUserId());
            if (!userContactList.contains(chatMessage.getContactId())) {
                // 如果不为该用户的联系人
                UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
                if (UserContactTypeEnum.USER == userContactTypeEnum) {
                    throw new BusinessException(ResponseCodeEnum.CODE_902);
                } else {
                    throw new BusinessException(ResponseCodeEnum.CODE_903);
                }
            }
        }

        LocalDateTime time = LocalDateTime.now();

        Integer messageType = chatMessage.getMessageType();
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageType);
        if (null == messageTypeEnum
                || !ArrayUtil.contains(
                new Integer[]{MessageTypeEnum.CHAT.getType(), MessageTypeEnum.MEDIA_CHAT.getType()},
                messageType)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ?
                MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDEND.getStatus();

        String sessionId = null;
        String sendUserId = userToken.getUserId();
        String contactId = chatMessage.getContactId();
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        switch (contactTypeEnum) {
            case USER:
                sessionId = StringTools.getChatSessionIdForUser(new String[]{sendUserId, contactId});
                break;
            case GROUP:
                sessionId = StringTools.getChatSessionIdForGroup(contactId);
                break;
        }

        String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());

        // 会话信息表，更新最新消息
        ChatSession chatSession = chatSessionService.getById(sessionId);
        chatSession.setLastMessage(messageContent);
        // 如果是群聊，则显示发送人昵称
        if (UserContactTypeEnum.GROUP == contactTypeEnum) {
            chatSession.setLastMessage(userToken.getUserId() + "：" + messageContent);
        }
        chatSession.setLastReceiveTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatSessionService.updateById(chatSession);

        // 消息聊天表
        chatMessage.setSessionId(sessionId);
        chatMessage.setSendTime(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        chatMessage.setStatus(status);
        chatMessage.setMessageContent(messageContent);
        chatMessage.setSendUserId(sendUserId);
        chatMessage.setSendUserNickName(userToken.getNickName());
        chatMessage.setContactType(contactTypeEnum.getType());
        this.save(chatMessage);

        // 发送消息
        MessageSendDTO messageSendDTO = BeanUtil.copyProperties(chatMessage, MessageSendDTO.class);

        // TODO 机器人返回的消息，接入gpt在此修改
        if (ROBOT_UID.equals(userToken.getUserId())) {
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            UserTokenDTO robot = new UserTokenDTO();

            robot.setUserId(sysSettingDTO.getRobotUid());
            robot.setNickName(sysSettingDTO.getRobotNicName());

            ChatMessage robotChatMessage = new ChatMessage();
            robotChatMessage.setContactId(sendUserId);
            robotChatMessage.setMessageContent("该版本是人工智障，此处待接入人工智能");
            robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
            saveMessage(robotChatMessage, robot);  // 回调函数，让机器人发送消息回复
        } else {
           messageHandler.sendMessage(messageSendDTO);
        }

        return messageSendDTO;
    }

    @Override
    public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {
        ChatMessage chatMessage = this.getById(messageId);
        // 判断会话信息是否存在
        if (chatMessage == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 判断是否为该用户的会话
        if (!chatMessage.getSendUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        // 获取文件后最名
        String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());

        if (!StrUtil.isEmpty(fileSuffix)
                && ArrayUtil.contains(IMAGE_SUFFIX_LIST, fileSuffix.toLowerCase())
                && file.getSize() > sysSettingDTO.getMaxImageSize() * FILE_SIZE_MB) {
            // 如果文件存在且为图片，大小超过2MB则抛异常
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        } else if (!StrUtil.isEmpty(fileSuffix)
                && ArrayUtil.contains(VIDEO_SUFFIX_LIST, fileSuffix.toLowerCase())
                && file.getSize() > sysSettingDTO.getMaxVideoSize() * FILE_SIZE_MB) {
            // 如果文件存在且为视频，大小超过5MB则抛异常
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        } else if (!StrUtil.isEmpty(fileSuffix)
                && !ArrayUtil.contains(IMAGE_SUFFIX_LIST, fileSuffix.toLowerCase())
                && !ArrayUtil.contains(VIDEO_SUFFIX_LIST, fileSuffix.toLowerCase())
                && file.getSize() > sysSettingDTO.getMaxFileSize() * FILE_SIZE_MB) {
            // 如果文件存在且不为图片或视频，大小超过5MB则抛异常
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 上传图片--上传到服务器
        String fileName = file.getOriginalFilename();
        String fileExtName = StringTools.getFileSuffix(fileName);
        String fileRealName = messageId + fileExtName;
        String month = DateUtil.format(new Date(chatMessage.getSendTime()), DateTimePatternEnum.YYYY_MM.getPattern());
        File folder = new File(commonConfig.getProjectFolder() + FILE_FOLDER_FILE + month);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File uploadFile = new File(folder.getPath() + "/" + fileRealName);
        try {
            file.transferTo(uploadFile);
            cover.transferTo(new File(uploadFile.getPath() + COVER_IMAGE_SUFFIX));  // 消息的封面
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new BusinessException("上传文件失败");
        }

        // 更改聊天消息表
        ChatMessage uploadInfo = query()
                .eq("message_id", messageId)
                .eq("status", MessageStatusEnum.SENDING.getStatus())  // 使用状态字段匹配
                .one();

        if (uploadInfo != null) {
            // 更新状态为已发送
            uploadInfo.setStatus(MessageStatusEnum.SENDEND.getStatus());

            // 更新数据库中的记录
            this.updateById(uploadInfo);  // 使用 updateById 更新记录
        }

        // 发送消息
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setStatus(MessageStatusEnum.SENDEND.getStatus());
        messageSendDTO.setMessageId(messageId);
        messageSendDTO.setMessageType(MessageTypeEnum.FILE_UPLOAD.getType());
        messageSendDTO.setContactId(chatMessage.getContactId());
        messageHandler.sendMessage(messageSendDTO);
    }

    @Override
    public File downloadFile(UserTokenDTO userToken, long messageId, Boolean showCover) {
        ChatMessage chatMessage = this.getById(messageId);
        String contactId = chatMessage.getContactId();
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (UserContactTypeEnum.USER == contactTypeEnum && !userToken.getUserId().equals(chatMessage.getContactId())) {
            // 如果是用户但不是登录用户
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (UserContactTypeEnum.GROUP == contactTypeEnum) {
            Long contactCount = userContactService.query()
                    .eq("user_id", userToken.getUserId())
                    .eq("contact_type", UserContactTypeEnum.GROUP.getType())
                    .eq("contact_id", contactId)
                    .eq("status", UserContactStatusEnum.FRIEND.getStatus())
                    .count();
            if (contactCount == 0) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        String month = DateUtil.format(
                new Date(chatMessage.getSendTime()),
                DateTimePatternEnum.YYYY_MM.getPattern());

        File folder = new File(commonConfig.getProjectFolder() + FILE_FOLDER_FILE + month);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = chatMessage.getFileName();
        String fileExtName = StringTools.getFileSuffix(fileName);
        String fileRealName = messageId + fileExtName;
        if (showCover != null && showCover) {
            fileRealName = fileRealName + COVER_IMAGE_SUFFIX;
        }
        // TODO File.separator解释，可替换 / ?
        File file = new File(folder.getPath() + "/" + fileRealName);
        if (!file.exists()) {
            log.info("文件不存在{}", messageId);
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        return file;
    }
}
