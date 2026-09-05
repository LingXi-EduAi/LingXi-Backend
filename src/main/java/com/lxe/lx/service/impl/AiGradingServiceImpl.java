package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.service.AiGradingService;
import com.lxe.lx.service.HomeworkSubmissionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 自动批改服务实现（BE-06）。
 * <p>
 * 通过 Dify Workflow（阻塞模式）对作业内容进行自动批改，
 * 解析返回的 grade/feedback 并回写到提交记录。
 * 仅在 ai.grading.enabled=true 时生效，失败仅记录日志，不影响主流程。
 * </p>
 */
@Service
public class AiGradingServiceImpl implements AiGradingService {

    private static final Logger logger = LogManager.getLogger(AiGradingServiceImpl.class);

    private final DifyGateway difyGateway;
    private final HomeworkSubmissionService homeworkSubmissionService;
    private final boolean enabled;

    public AiGradingServiceImpl(
            DifyGateway difyGateway,
            HomeworkSubmissionService homeworkSubmissionService,
            @Value("${ai.grading.enabled:false}") boolean enabled) {
        this.difyGateway = difyGateway;
        this.homeworkSubmissionService = homeworkSubmissionService;
        this.enabled = enabled;
    }

    @Override
    public void grade(HomeworkSubmission submission) {
        // 未开启 AI 批改：保持原行为，不调用 Dify、不修改任何数据
        if (!enabled) {
            return;
        }
        if (submission == null || StringUtils.isBlank(submission.getId())) {
            logger.warn("AI 批改跳过：提交记录为空或缺少 id");
            return;
        }
        try {
            // 构建 Workflow 输入（content + assignmentId）
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("content", StringUtils.defaultString(submission.getContent()));
            inputs.put("assignmentId", StringUtils.defaultString(submission.getAssignmentId()));

            // 阻塞调用 Dify Workflow 获取批改结果
            JsonNode result = difyGateway.runWorkflow(inputs, submission.getStudentId());

            // 解析批改结果：Dify Workflow 阻塞响应结构为 data.outputs
            JsonNode outputs = result.path("data").path("outputs");
            int aiGrade = outputs.path("grade").asInt(-1);
            String aiFeedback = outputs.path("feedback").asText("");
            if (aiGrade < 0) {
                logger.warn("AI 批改结果缺少有效 grade，跳过更新：submissionId={}", submission.getId());
                return;
            }

            // 重新读取最新记录，避免与手动批改的版本号冲突（gradeHomework 按 version 乐观锁更新）
            HomeworkSubmission latest = homeworkSubmissionService.getById(submission.getId());
            if (latest == null) {
                logger.warn("AI 批改跳过：提交记录不存在或已删除：submissionId={}", submission.getId());
                return;
            }
            latest.setGrade(aiGrade);
            latest.setFeedback(aiFeedback);
            homeworkSubmissionService.gradeHomework(latest);
            logger.info("AI 批改完成：submissionId={}, grade={}", submission.getId(), aiGrade);
        } catch (Exception e) {
            // AI 批改为 best-effort 增强：失败仅记录日志，保持原批改结果不变
            logger.error("AI 批改失败，保持原批改结果不变：submissionId=" + submission.getId(), e);
        }
    }
}