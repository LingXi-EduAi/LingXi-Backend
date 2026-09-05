package com.lxe.lx.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 成绩分布 VO
 * <p>
 * 对应 /analytics/stats 返回 data.gradeDistribution 的 JSON 结构。
 */
@Data
public class GradeDistributionVO {

    /** 成绩段标签，如 ["优秀(90-100)", "良好(80-89)", "及格(60-79)", "不及格(<60)"] */
    private List<String> labels;

    /** 各成绩段人数，与 labels 一一对应 */
    private List<Integer> data;
}