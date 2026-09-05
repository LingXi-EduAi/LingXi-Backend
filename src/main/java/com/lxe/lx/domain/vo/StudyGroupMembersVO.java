package com.lxe.lx.domain.vo;

import com.lxe.lx.pojo.StudyGroupMember;
import lombok.Data;

import java.util.List;

/**
 * 学习小组成员列表 VO
 * <p>
 * 对应 /studyGroup/members 返回 data 的 JSON 结构（list）。
 */
@Data
public class StudyGroupMembersVO {

    /** 成员列表 */
    private List<StudyGroupMember> list;
}