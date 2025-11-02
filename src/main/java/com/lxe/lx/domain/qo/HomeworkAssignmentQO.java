package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeworkAssignmentQO extends BaseListQO {
    private String teacherId;    // 教师id
    private String classId;      // 班级id
    private String subject;      // 学科
    private String status;       // 状态
    private String title;        // 标题（用于搜索）
}



