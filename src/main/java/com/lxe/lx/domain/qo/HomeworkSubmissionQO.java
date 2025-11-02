package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeworkSubmissionQO extends BaseListQO {
    private String assignmentId;    // 作业发布id
    private String studentId;       // 学生id
    private String status;          // 状态
    private String teacherId;       // 教师id（用于教师查看所有提交）
}



