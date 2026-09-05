package com.lxe.lx.domain.vo;

import com.lxe.lx.pojo.StudyGroup;
import lombok.Data;

import java.util.List;

/**
 * 学习小组列表 VO
 * <p>
 * 对应 /studyGroup/list 返回 data 的 JSON 结构（list / count）。
 */
@Data
public class StudyGroupListVO {

    /** 小组列表 */
    private List<StudyGroup> list;

    /** 小组总数 */
    private Integer count;
}