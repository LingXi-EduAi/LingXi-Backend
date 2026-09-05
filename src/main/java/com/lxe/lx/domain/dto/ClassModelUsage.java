package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 教师按班级维度查看学生 AI 用量聚合结果（BE-08）。
 * 包含班级整体汇总 + 班内每个学生的分批汇总。
 */
@Getter
@Setter
public class ClassModelUsage {
    private String classId;
    private String className;
    /** 班内学生总数（含无 AI 用量的学生）。 */
    private int studentCount;
    /** 有 AI 调用记录的学生数。 */
    private int activeStudentCount;
    /** 班级整体汇总。 */
    private long totalTokens;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private long averageLatencyMs;
    private long failedCount;
    private List<StudentUsageDTO> students = new ArrayList<>();
}
