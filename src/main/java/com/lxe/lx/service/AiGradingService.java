package com.lxe.lx.service;

import com.lxe.lx.pojo.HomeworkSubmission;

/**
 * AI 自动批改服务（BE-06）。
 * <p>
 * 作为手动批改的异步增强：开启 ai.grading.enabled 后，
 * 手动批改成功会触发 AI Workflow 批改并回写 grade/feedback。
 * 批改为 best-effort，任何失败都不影响主流程。
 * </p>
 */
public interface AiGradingService {

    /**
     * 对提交记录执行 AI 批改。
     * 未开启配置时直接返回；开启后调用 Dify Workflow 并回写批改结果。
     *
     * @param submission 已提交的作业记录（至少包含 id、content、assignmentId、studentId）
     */
    void grade(HomeworkSubmission submission);
}