package online.happlay.chat.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import online.happlay.chat.annotation.GlobalInterceptor;
import online.happlay.chat.entity.dto.SaveGroupDTO;
import online.happlay.chat.entity.dto.UserTokenDTO;
import online.happlay.chat.entity.page.GroupPageQuery;
import online.happlay.chat.entity.po.GroupInfo;
import online.happlay.chat.entity.vo.GroupDetails;
import online.happlay.chat.entity.vo.GroupInfoVO;
import online.happlay.chat.entity.vo.MyGroups;
import online.happlay.chat.entity.vo.ResponseVO;
import online.happlay.chat.service.IGroupInfoService;
import org.jetbrains.annotations.Nullable;
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
    public ResponseVO loadMyGroup(HttpServletRequest request) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        List<MyGroups> groupPageQueries = groupInfoService.getMyGroups(userToken);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupPageQueries);
    }

    @ApiOperation("查看群组的详细信息")
    @PostMapping("/getGroupInfo")
    @GlobalInterceptor
    public ResponseVO getGroupInfo(HttpServletRequest request, @RequestParam("groupId") @NotEmpty String groupId) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        GroupDetails groupDetails = groupInfoService.getGroupInfo(userToken, groupId);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupDetails);
    }

    @ApiOperation("查看群组的成员信息")
    @PostMapping("/getGroupInfo4Chat")
    @GlobalInterceptor
    public ResponseVO getGroupMember(HttpServletRequest request, @RequestParam("groupId") @NotEmpty String groupId) {
        // 获取当前用户的 token 对象
        UserTokenDTO userToken = getUserToken(request);
        GroupInfoVO groupInfoVO = groupInfoService.getGroupMember(userToken, groupId);
        // 返回封装的响应结果
        return getSuccessResponseVO(groupInfoVO);
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
