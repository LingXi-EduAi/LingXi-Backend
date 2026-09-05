package com.lxe.lx.domain.vo;

import lombok.Data;

/**
 * 薄弱知识点 VO
 * <p>
 * 对应 /analytics/stats 返回 data.weakPoints 数组元素结构。
 */
@Data
public class WeakPointVO {

    /** 学科 */
    private String subject;

    /** 作业标题 */
    private String title;

    /** 平均分（保留一位小数） */
    private Double avgScore;

    /** 提交人数 */
    private Integer submissionCount;

    /** 作业 ID */
    private String assignmentId;

    /** 教学建议 */
    private String suggestion;
}