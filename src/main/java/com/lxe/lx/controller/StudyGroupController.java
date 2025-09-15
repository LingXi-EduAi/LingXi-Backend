package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.pojo.StudyGroup;
import com.lxe.lx.pojo.StudyGroupMember;
import com.lxe.lx.pojo.StudyGroupMessage;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.StudyGroupService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/studyGroup")
public class StudyGroupController {
    Logger logger = LogManager.getLogger(StudyGroupController.class);

    @Autowired
    private StudyGroupService studyGroupService;

    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody StudyGroup group) {
        if (group == null || StringUtils.isBlank(group.getName())) {
            return ResultConstant.illegalParams("小组名称不能为空");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            if (token == null) {
                return ResultConstant.error("用户未登录或登录已过期");
            }
            group.setId(Tools.get32UUID());
            group.setCreateId(token.getId());
            group.setCreateTime(Tools.nowTimeStr());
            group.setState("1");
            group.setVersion(1);
            return studyGroupService.addGroup(group);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.add error:" + e.getMessage());
            return ResultConstant.error("创建小组失败");
        }
    }

    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody StudyGroup group) {
        if (group == null || StringUtils.isBlank(group.getId())) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            group.setUpdateId(token.getId());
            group.setUpdateTime(Tools.nowTimeStr());
            return studyGroupService.editGroup(group);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.edit error:" + e.getMessage());
            return ResultConstant.error("编辑小组失败");
        }
    }

    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String state,
                               @RequestParam(defaultValue = "1") String pageType,
                               @RequestParam(defaultValue = "0") Integer start,
                               @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", name);
            params.put("category", category);
            params.put("state", state);
            params.put("pageType", pageType);
            params.put("start", start);
            params.put("pageSize", pageSize);
            List<StudyGroup> list = studyGroupService.listGroup(params);
            Map<String, Object> dto = new HashMap<>();
            dto.put("list", list);
            dto.put("count", studyGroupService.numGroup(params));
            return ResultConstant.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.list error:" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    @Login
    @RequestMapping(value = "/join", method = RequestMethod.POST)
    public ResultConstant join(HttpServletRequest request, @RequestParam String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return ResultConstant.illegalParams("groupId不能为空");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            StudyGroupMember member = new StudyGroupMember();
            member.setId(Tools.get32UUID());
            member.setGroupId(groupId);
            member.setCustomerId(token.getId());
            member.setRole("member");
            member.setJoinTime(Tools.nowTimeStr());
            member.setState("1");
            member.setVersion(1);
            return studyGroupService.joinGroup(member);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.join error:" + e.getMessage());
            return ResultConstant.error("加入失败");
        }
    }

    @Login
    @RequestMapping(value = "/leave", method = RequestMethod.POST)
    public ResultConstant leave(HttpServletRequest request, @RequestParam String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return ResultConstant.illegalParams("groupId不能为空");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            StudyGroupMember member = new StudyGroupMember();
            member.setGroupId(groupId);
            member.setCustomerId(token.getId());
            return studyGroupService.leaveGroup(member);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.leave error:" + e.getMessage());
            return ResultConstant.error("退出失败");
        }
    }

    @Login
    @RequestMapping(value = "/kick", method = RequestMethod.POST)
    public ResultConstant kick(HttpServletRequest request, @RequestParam String groupId, @RequestParam String memberId) {
        if (StringUtils.isBlank(groupId)) {
            return ResultConstant.illegalParams("groupId不能为空");
        }
        if (StringUtils.isBlank(memberId)) {
            return ResultConstant.illegalParams("memberId不能为空");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            
            // 检查操作者是否是小组创建者
            StudyGroup group = studyGroupService.getGroupById(groupId);
            if (group == null) {
                return ResultConstant.error("小组不存在");
            }
            if (!token.getId().equals(group.getCreateId())) {
                return ResultConstant.error("只有创建者可以踢出成员");
            }
            
            // 不能踢出自己
            if (token.getId().equals(memberId)) {
                return ResultConstant.error("不能踢出自己");
            }
            
            StudyGroupMember member = new StudyGroupMember();
            member.setGroupId(groupId);
            member.setCustomerId(memberId);
            return studyGroupService.leaveGroup(member);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.kick error:" + e.getMessage());
            return ResultConstant.error("踢出失败");
        }
    }

    @Login
    @RequestMapping(value = "/members", method = RequestMethod.POST)
    public ResultConstant members(HttpServletRequest request, @RequestParam String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return ResultConstant.illegalParams("groupId不能为空");
        }
        try {
            List<StudyGroupMember> list = studyGroupService.listMembers(groupId);
            Map<String, Object> dto = new HashMap<>();
            dto.put("list", list);
            return ResultConstant.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.members error:" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    @Login
    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public ResultConstant send(HttpServletRequest request, @RequestParam String groupId, @RequestParam String content) {
        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(content)) {
            return ResultConstant.illegalParams("参数错误");
        }
        try {
            TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            StudyGroupMessage message = new StudyGroupMessage();
            message.setId(Tools.get32UUID());
            message.setGroupId(groupId);
            message.setSenderId(token.getId());
            message.setContent(content);
            message.setCreateTime(Tools.nowTimeStr());
            message.setState("1");
            message.setVersion(1);
            return studyGroupService.addMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.send error:" + e.getMessage());
            return ResultConstant.error("发送失败");
        }
    }

    @Login
    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    public ResultConstant messages(HttpServletRequest request,
                                   @RequestParam String groupId,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime,
                                   @RequestParam(defaultValue = "1") String pageType,
                                   @RequestParam(defaultValue = "0") Integer start,
                                   @RequestParam(defaultValue = "50") Integer pageSize) {
        if (StringUtils.isBlank(groupId)) {
            return ResultConstant.illegalParams("groupId不能为空");
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("groupId", groupId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            params.put("pageType", pageType);
            params.put("start", start);
            params.put("pageSize", pageSize);
            List<StudyGroupMessage> list = studyGroupService.listMessages(params);
            Map<String, Object> dto = new HashMap<>();
            dto.put("list", list);
            return ResultConstant.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("studyGroup.messages error:" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
}
