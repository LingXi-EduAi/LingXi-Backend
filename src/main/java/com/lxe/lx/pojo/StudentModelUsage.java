package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 单个学生在某班级内的 AI 模型调用聚合结果（BE-08）。
 * 对应 ai_model_call_log 关联 ai_task / lx_customer 后按 user_id 分组的汇总行。
 */
@Getter
@Setter
public class StudentModelUsage {
    private String userId;
    private Long callCount;
    private Long totalTokens;
    private Long totalLatencyMs;
    private BigDecimal totalCost;
    private Long failedCount;
}
