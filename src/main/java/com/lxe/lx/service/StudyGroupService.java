package com.lxe.lx.service;

import com.lxe.lx.pojo.StudyGroup;
import com.lxe.lx.pojo.StudyGroupMember;
import com.lxe.lx.pojo.StudyGroupMessage;
import com.lxe.lx.util.ResultConstant;

import java.util.List;
import java.util.Map;

public interface StudyGroupService {
    ResultConstant addGroup(StudyGroup group) throws Exception;
    ResultConstant editGroup(StudyGroup group) throws Exception;
    ResultConstant deleteGroup(String id) throws Exception;
    StudyGroup getGroupById(String id) throws Exception;
    int numGroup(Map<String, Object> params) throws Exception;
    List<StudyGroup> listGroup(Map<String, Object> params) throws Exception;

    ResultConstant joinGroup(StudyGroupMember member) throws Exception;
    ResultConstant leaveGroup(StudyGroupMember member) throws Exception;
    List<StudyGroupMember> listMembers(String groupId) throws Exception;

    ResultConstant addMessage(StudyGroupMessage message) throws Exception;
    List<StudyGroupMessage> listMessages(Map<String, Object> params) throws Exception;
    StudyGroupMessage getMessageById(String id) throws Exception;
    int countMessages(Map<String, Object> params) throws Exception;
}
