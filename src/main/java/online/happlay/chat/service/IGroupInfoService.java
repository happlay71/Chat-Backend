package online.happlay.chat.service;

import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.page.GroupPageQuery;
import online.happlay.chat.entity.po.GroupInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import online.happlay.chat.entity.vo.GroupDetails;
import online.happlay.chat.entity.vo.GroupInfoVO;
import online.happlay.chat.entity.vo.MyGroups;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 群 服务类
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
public interface IGroupInfoService extends IService<GroupInfo> {

    /**
     * 创建群组
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     */
    void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

    void updateGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

    List<MyGroups> getMyGroups(UserTokenDTO userToken);

    GroupDetails getGroupInfo(UserTokenDTO userToken, String groupId);

    GroupInfoVO getGroupMember(UserTokenDTO userToken, String groupId);
}
