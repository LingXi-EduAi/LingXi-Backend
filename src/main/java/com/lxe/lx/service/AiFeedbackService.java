package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiFeedbackView;

/**
 * AI 个性化反馈服务（BE-15）。
 *
 * <p>任务完成后基于 {@code validation_finished} 反思事件生成"薄弱点 + 推荐练习"；
 * 当反思事件缺失时按本地规则生成，并标记 {@code source=RULE_FALLBACK}。
 * 每个任务至多一条反馈，读取时若不存在则惰性生成，保证幂等。
 */
public interface AiFeedbackService {

    /** 校验任务所有权后读取反馈；不存在时惰性生成并落库。 */
    AiFeedbackView getOrGenerate(String taskId, String userId);

    /** 生成并落库反馈（不做所有权校验，供后端内部调用）；任务不存在返回 null。 */
    AiFeedbackView generateForTask(String taskId);
}
