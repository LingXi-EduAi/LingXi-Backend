package com.lxe.lx.domain.vo;

import lombok.Data;

/**
 * 优秀学生 VO
 * <p>
 * 对应 /analytics/stats 返回 data.topStudents 数组元素结构。
 */
@Data
public class TopStudentVO {

    /** 学生姓名 */
    private String name;

    /** 平均分（四舍五入取整） */
    private Integer score;

    /** 进步幅度（示例数据） */
    private String improvement;

    /** 优势描述 */
    private String strengths;
}