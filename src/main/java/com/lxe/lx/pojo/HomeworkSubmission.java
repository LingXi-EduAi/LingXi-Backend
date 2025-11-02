package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * 作业提交实体类（学生提交的作业）
 */
@Getter
@Setter
public class HomeworkSubmission {
    private String id;
    private String assignmentId;       // 作业发布id
    private String studentId;          // 学生id
    private String studentName;        // 学生姓名
    private String content;            // 提交内容
    private String fileAddress;        // 附件地址
    private String submitTime;         // 提交时间
    private Integer grade;             // 成绩
    private String feedback;           // 教师反馈
    private String gradedTime;         // 批改时间
    private String gradedBy;           // 批改教师id
    private String status;             // 状态：pending-待提交, submitted-已提交, graded-已批改
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;              // 数据状态：0-删除，1-正常
    private Integer version;           // 版本号
}



