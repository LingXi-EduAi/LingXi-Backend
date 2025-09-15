package com.lxe.lx.mapper;

import com.lxe.lx.pojo.StudyGroup;
import com.lxe.lx.pojo.StudyGroupMember;
import com.lxe.lx.pojo.StudyGroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface StudyGroupMapper {
    // groups
    void addGroup(StudyGroup group);
    void editGroup(StudyGroup group);
    StudyGroup getGroupById(String id);
    int numGroup(Map<String, Object> params);
    List<StudyGroup> listGroup(Map<String, Object> params);
    void deleteGroup(String id);

    // members
    void addMember(StudyGroupMember member);
    void removeMember(StudyGroupMember member);
    List<StudyGroupMember> listMembers(String groupId);
    int countMember(StudyGroupMember member);

    // messages
    void addMessage(StudyGroupMessage message);
    List<StudyGroupMessage> listMessages(Map<String, Object> params);
    StudyGroupMessage getMessageById(String id);
    int countMessages(Map<String, Object> params);
}
