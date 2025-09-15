package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.StudyGroupMapper;
import com.lxe.lx.pojo.StudyGroup;
import com.lxe.lx.pojo.StudyGroupMember;
import com.lxe.lx.pojo.StudyGroupMessage;
import com.lxe.lx.service.StudyGroupService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("StudyGroupService")
public class StudyGroupServiceImpl implements StudyGroupService {
    @Autowired
    private StudyGroupMapper studyGroupMapper;

    @Override
    public ResultConstant addGroup(StudyGroup group) throws Exception {
        try {
            studyGroupMapper.addGroup(group);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public ResultConstant editGroup(StudyGroup group) throws Exception {
        try {
            studyGroupMapper.editGroup(group);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public ResultConstant deleteGroup(String id) throws Exception {
        try {
            studyGroupMapper.deleteGroup(id);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public StudyGroup getGroupById(String id) throws Exception {
        return studyGroupMapper.getGroupById(id);
    }

    @Override
    public int numGroup(Map<String, Object> params) throws Exception {
        return studyGroupMapper.numGroup(params);
    }

    @Override
    public List<StudyGroup> listGroup(Map<String, Object> params) throws Exception {
        return studyGroupMapper.listGroup(params);
    }

    @Override
    public ResultConstant joinGroup(StudyGroupMember member) throws Exception {
        try {
            int exist = studyGroupMapper.countMember(member);
            if (exist > 0) {
                return ResultConstant.success("已在小组中");
            }
            studyGroupMapper.addMember(member);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public ResultConstant leaveGroup(StudyGroupMember member) throws Exception {
        try {
            studyGroupMapper.removeMember(member);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public List<StudyGroupMember> listMembers(String groupId) throws Exception {
        return studyGroupMapper.listMembers(groupId);
    }

    @Override
    public ResultConstant addMessage(StudyGroupMessage message) throws Exception {
        try {
            // 内容安全检查（简单实现）
            if (message.getContent() != null && message.getContent().length() > 500) {
                return ResultConstant.error("消息内容过长，请控制在500字符以内");
            }
            
            studyGroupMapper.addMessage(message);
            return ResultConstant.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public List<StudyGroupMessage> listMessages(Map<String, Object> params) throws Exception {
        return studyGroupMapper.listMessages(params);
    }

    @Override
    public StudyGroupMessage getMessageById(String id) throws Exception {
        return studyGroupMapper.getMessageById(id);
    }

    @Override
    public int countMessages(Map<String, Object> params) throws Exception {
        return studyGroupMapper.countMessages(params);
    }
}
