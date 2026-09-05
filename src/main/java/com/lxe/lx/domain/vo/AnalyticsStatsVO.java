package com.lxe.lx.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 学情分析统计数据 VO
 * <p>
 * 对应 /analytics/stats 返回 data 的 JSON 结构。
 */
@Data
public class AnalyticsStatsVO {

    /** 完成率（百分比，四舍五入取整） */
    private Long completionRate;

    /** 平均分（保留一位小数） */
    private Double averageScore;

    /** 总提交数 */
    private Integer totalSubmissions;

    /** 已批改数 */
    private Integer gradedCount;

    /** 成绩分布 */
    private GradeDistributionVO gradeDistribution;

    /** 优秀学生列表（前 5 名） */
    private List<TopStudentVO> topStudents;

    /** 需要关注的学生列表 */
    private List<StudentNeedAttentionVO> studentsNeedAttention;

    /** 薄弱知识点列表 */
    private List<WeakPointVO> weakPoints;

    /** 薄弱知识点数量 */
    private Integer weakPointsCount;
}