package online.happlay.chat.controller;


import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.config.CommonConfig;
import online.happlay.chat.constants.Constants;
import online.happlay.chat.entity.dto.message.ChatSendMessageDTO;
import online.happlay.chat.entity.dto.message.MessageSendDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.ChatMessage;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.entity.vo.user.UserInfoVO;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.enums.message.MessageTypeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.service.IChatMessageService;
import online.happlay.chat.service.IChatSessionUserService;
import online.happlay.chat.utils.StringTools;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import static online.happlay.chat.constants.Constants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "聊天")
@RequestMapping("/chat")
public class ChatController extends BaseController {

    private final IChatMessageService chatMessageService;

    private final IChatSessionUserService chatSessionUserService;

    private final CommonConfig commonConfig;

    @ApiOperation("向联系人发送消息")
    @GetMapping("/sendMessage")
    @GlobalInterceptor
    public ResponseVO<MessageSendDTO> sendMessage(HttpServletRequest request,
                                                  @Validated  @ModelAttribute ChatSendMessageDTO chatSendMessageDTO) {


        UserTokenDTO userToken = getUserToken(request);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContactId(chatSendMessageDTO.getContactId());
        chatMessage.setMessageContent(chatSendMessageDTO.getMessageContent());
        chatMessage.setMessageType(chatSendMessageDTO.getMessageType());
        chatMessage.setFileSize(chatSendMessageDTO.getFileSize());
        chatMessage.setFileName(chatSendMessageDTO.getFileName());
        chatMessage.setFileType(chatSendMessageDTO.getFileType());
        MessageSendDTO messageSendDTO = chatMessageService.saveMessage(chatMessage, userToken);

        return getSuccessResponseVO(messageSendDTO);
    }

    @ApiOperation("上传媒体文件到服务器")
    @GetMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpServletRequest request,
                                 @RequestParam("messageId") Long messageId,
                                 @RequestPart(value = "file") MultipartFile file,
                                 @RequestPart(value = "cover") MultipartFile cover) {
        UserTokenDTO userToken = getUserToken(request);
        chatMessageService.saveMessageFile(userToken.getUserId(), messageId, file, cover);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("从服务器下载媒体文件")
    @GetMapping("/downloadFile")
    @GlobalInterceptor
    public void downloadFile(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam("fileId") @NotEmpty String fileId,
                                   @RequestParam("showCover") Boolean showCover) {
        UserTokenDTO userToken = getUserToken(request);
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = null;
            if (!StringTools.isNumber(fileId)) {
                // 如果不是数字
                String avatarFolderName = FILE_FOLDER_FILE + FILE_FOLDER_AVATAR_NAME;
                String avatarPath = commonConfig.getProjectFolder() + avatarFolderName + fileId + IMAGE_SUFFIX;
                if (showCover) {
                    avatarPath = avatarPath + COVER_IMAGE_SUFFIX;
                }
                file = new File(avatarPath);
                if (!file.exists()) {
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            } else {
                file = chatMessageService.downloadFile(userToken, Long.parseLong(fileId), showCover);
            }

            response.setContentType("application/x-msdownload;charset=UTF-8");  // 通用的文件下载 MIME 类型，告诉客户端应该将响应处理为文件下载
            response.setHeader("Content-Disposition", "attachment;");  // 意味着这是一个附件，客户端通常会弹出文件下载对话框，而不是直接显示文件
            response.setContentLengthLong(file.length());  // 设置为文件的大小，告诉客户端文件的总长度，以便正确处理文件下载进度

            /**
             * FileInputStream 用于从服务器本地的文件系统读取文件内容。
             * 使用 byte[] 缓冲区按 1024 字节块读取文件，并将其写入到响应的输出流 out，逐步将文件内容发送给客户端。
             * while 循环: 读取文件直至文件的所有字节都被传输给客户端。
             * out.flush()：确保所有数据都被输出到客户端。
             */
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();

        } catch (Exception e) {
            log.error("下载文件失败", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    log.error("IO异常", e);
                }
            }

            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    log.error("IO异常", e);
                }
            }
        }
    }

}
