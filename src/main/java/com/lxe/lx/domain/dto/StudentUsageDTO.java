package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 班级内单个学生的 AI 用量聚合（BE-08）。
 */
@Getter
@Setter
public class StudentUsageDTO {
    private String studentId;
    private String studentName;
    private long callCount;
    private long totalTokens;
    private long totalLatencyMs;
    private long averageLatencyMs;
    private BigDecimal totalCost;
    private long failedCount;
}
