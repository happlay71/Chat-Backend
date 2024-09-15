package online.happlay.chat.controller;


import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.group.LoadGroupQueryDTO;
import online.happlay.chat.entity.dto.group.SaveGroupDTO;
import online.happlay.chat.entity.dto.user.UserTokenDTO;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.vo.common.ResponseVO;
import online.happlay.chat.entity.vo.group.GroupDetails;
import online.happlay.chat.entity.vo.group.GroupInfoVO;
import online.happlay.chat.entity.vo.group.MyGroups;
import online.happlay.chat.entity.vo.page.PaginationResultVO;
import online.happlay.chat.enums.ResponseCodeEnum;
import online.happlay.chat.exception.BusinessException;
import online.happlay.chat.service.IGroupInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 群 前端控制器
 * </p>
 *
 * @author happlay
 * @since 2024-08-26
 */
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Api(tags = "群组信息")
public class GroupInfoController extends BaseController {

    private final IGroupInfoService groupInfoService;

    @ApiOperation("创建或修改群组")
    @PostMapping("/saveGroup")
    @GlobalInterceptor
    public ResponseVO saveGroup(@Validated @ModelAttribute SaveGroupDTO saveGroupDTO,
                                @RequestPart("avatarFile") MultipartFile avatarFile,
                                @RequestPart("avatarCover") MultipartFile avatarCover,
                                HttpServletRequest request) throws IOException {
        UserTokenDTO userToken = getUserToken(request);
        GroupInfo groupInfo = createGroupInfo(saveGroupDTO, userToken);
        if (StrUtil.isEmpty(saveGroupDTO.getGroupId())) {
            groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);
        } else {
            groupInfoService.updateGroup(groupInfo, avatarFile, avatarCover);
        }
        return getSuccessResponseVO(null);
    }

    @ApiOperation("查看我创建的群组")
    @GetMapping("/loadMyGroup")
    @GlobalInterceptor
    public ResponseVO<List<MyGroups>> loadMyGroup(HttpServletRequest request) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        List<MyGroups> groupPageQueries = groupInfoService.getMyGroups(userToken);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupPageQueries);
    }

    @ApiOperation("查看群组的详细信息")
    @PostMapping("/getGroupInfo")
    @GlobalInterceptor
    public ResponseVO<GroupDetails> getGroupInfo(HttpServletRequest request, @RequestParam("groupId") @NotEmpty String groupId) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        GroupDetails groupDetails = groupInfoService.getGroupInfo(userToken, groupId);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupDetails);
    }

    @ApiOperation("查看群组的成员信息")
    @PostMapping("/getGroupInfo4Chat")
    @GlobalInterceptor
    public ResponseVO<GroupInfoVO> getGroupMember(HttpServletRequest request, @RequestParam("groupId") @NotEmpty String groupId) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        GroupInfoVO groupInfoVO = groupInfoService.getGroupMember(userToken, groupId);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupInfoVO);
    }

    @ApiOperation("管理员加载所有群组信息")
    @GetMapping("/loadGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO<PaginationResultVO<GroupDetails>> loadGroup(@ModelAttribute LoadGroupQueryDTO loadGroupQueryDTO) {
        PaginationResultVO<GroupDetails> resultVO = groupInfoService.loadGroup(loadGroupQueryDTO);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("管理员解散群组")
    @PostMapping("/dissolutionGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO dissolutionGroup(@RequestParam("groupId") @NotEmpty String groupId) {
        GroupInfo groupInfo = groupInfoService.getById(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 获取群主ID
        String groupOwnerId = groupInfo.getGroupOwnerId();

        groupInfoService.dissolutionGroup(groupId, groupOwnerId);
        return getSuccessResponseVO(null);
    }






    private static GroupInfo createGroupInfo(SaveGroupDTO saveGroupDTO, UserTokenDTO userToken) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(saveGroupDTO.getGroupId());
        groupInfo.setGroupOwnerId(userToken.getUserId());
        groupInfo.setGroupName(saveGroupDTO.getGroupName());
        groupInfo.setGroupNotice(saveGroupDTO.getGroupNotice());
        groupInfo.setJoinType(saveGroupDTO.getJoinType());
        return groupInfo;
    }
}
