package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * 作业发布实体类（教师布置的作业）
 */
@Getter
@Setter
public class HomeworkAssignment {
    private String id;
    private String title;              // 作业标题
    private String content;            // 作业内容
    private String subject;            // 学科
    private String classId;            // 班级id
    private String teacherId;          // 教师id
    private String teacherName;        // 教师姓名
    private String startTime;          // 开始时间
    private String endTime;            // 截止时间
    private String status;             // 状态：pending-未开始, progress-进行中, ended-已结束
    private String fileAddress;        // 附件地址
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;              // 数据状态：0-删除，1-正常
    private Integer version;           // 版本号
}



