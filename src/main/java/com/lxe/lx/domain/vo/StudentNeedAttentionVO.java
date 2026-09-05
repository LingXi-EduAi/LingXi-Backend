package com.lxe.lx.domain.vo;

import lombok.Data;

/**
 * 需要关注的学生 VO
 * <p>
 * 对应 /analytics/stats 返回 data.studentsNeedAttention 数组元素结构。
 */
@Data
public class StudentNeedAttentionVO {

    /** 学生姓名 */
    private String name;

    /** 平均分（四舍五入取整） */
    private Integer score;

    /** 薄弱点描述 */
    private String weakness;

    /** 建议 */
    private String suggestion;
}