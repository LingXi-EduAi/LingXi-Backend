package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.AiFeedbackView;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiEvidenceMapper;
import com.lxe.lx.mapper.AiFeedbackMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiFeedback;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiFeedbackService;
import com.lxe.lx.service.AiTaskApiException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * BE-15 个性化反馈实现。
 *
 * <p>优先消费任务的 {@code validation_finished} 事件（反思 Agent 结构化输出：
 * {@code conclusion / checks / suggestions}）；无该事件时按本地规则基于真实回答、
 * 引用来源和提问内容生成，避免固定文案。每个任务唯一一行，读取惰性生成。
 */
@Service
public class AiFeedbackServiceImpl implements AiFeedbackService {

    private static final Logger logger = LogManager.getLogger(AiFeedbackServiceImpl.class);

    private static final String SOURCE_VALIDATION = "VALIDATION_FINISHED";
    private static final String SOURCE_RULE = "RULE_FALLBACK";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final int MAX_ITEMS = 5;

    private final AiFeedbackMapper feedbackMapper;
    private final AiTaskMapper taskMapper;
    private final AiEventMapper eventMapper;
    private final AiMessageMapper messageMapper;
    private final AiEvidenceMapper evidenceMapper;
    private final ObjectMapper objectMapper;

    public AiFeedbackServiceImpl(AiFeedbackMapper feedbackMapper,
                                 AiTaskMapper taskMapper,
                                 AiEventMapper eventMapper,
                                 AiMessageMapper messageMapper,
                                 AiEvidenceMapper evidenceMapper,
                                 ObjectMapper objectMapper) {
        this.feedbackMapper = feedbackMapper;
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.messageMapper = messageMapper;
        this.evidenceMapper = evidenceMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AiFeedbackView getOrGenerate(String taskId, String userId) {
        AiTask task = taskMapper.findByIdAndUser(taskId, userId);
        if (task == null) {
            throw new AiTaskApiException(404, "AI 任务不存在");
        }
        return generateFor(task);
    }

    @Override
    @Transactional
    public AiFeedbackView generateForTask(String taskId) {
        AiTask task = taskMapper.findById(taskId);
        if (task == null) {
            return null;
        }
        return generateFor(task);
    }

    private AiFeedbackView generateFor(AiTask task) {
        AiFeedback existing = feedbackMapper.findByTaskId(task.getId());
        if (existing != null) {
            return toView(existing);
        }
        try {
            return toView(insert(task));
        } catch (DuplicateKeyException raced) {
            AiFeedback concurrent = feedbackMapper.findByTaskId(task.getId());
            return concurrent == null ? null : toView(concurrent);
        }
    }

    private AiFeedback insert(AiTask task) {
        Reflection reflection = loadReflection(task.getId());
        AiMessage answerMessage = messageMapper.findByTaskAndRole(task.getId(), ASSISTANT_ROLE);
        List<AiEvidence> evidences = answerMessage == null
                ? Collections.emptyList()
                : evidenceMapper.findByMessageId(answerMessage.getId());
        String query = extractQuery(task.getRequestJson());

        FeedbackPlan plan = buildPlan(reflection, query, answerMessage, evidences);

        AiFeedback feedback = new AiFeedback();
        feedback.setId(UUID.randomUUID().toString().replace("-", ""));
        feedback.setTaskId(task.getId());
        feedback.setConversationId(task.getConversationId());
        feedback.setUserId(task.getUserId());
        feedback.setSummary(plan.summary);
        feedback.setWeakPointsJson(writeJson(plan.weakPoints));
        feedback.setRecommendationsJson(writeJson(plan.recommendations));
        feedback.setNextStep(plan.nextStep);
        feedback.setSource(plan.source);
        feedback.setRawPayloadJson(reflection == null ? null : reflection.rawPayload);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return feedback;
    }

    private FeedbackPlan buildPlan(Reflection reflection, String query,
                                   AiMessage answerMessage, List<AiEvidence> evidences) {
        FeedbackPlan plan = new FeedbackPlan();
        String answer = answerMessage == null ? null : answerMessage.getContent();
        if (reflection != null) {
            plan.source = SOURCE_VALIDATION;
            plan.summary = StringUtils.defaultIfBlank(reflection.conclusion,
                    "本次任务的反思校验已完成。");
            plan.weakPoints = limit(reflection.weakPoints);
            plan.recommendations = limit(reflection.suggestions);
            plan.nextStep = StringUtils.defaultIfBlank(reflection.nextStep,
                    "先复习薄弱点，再独立完成推荐练习。");
        } else {
            plan.source = SOURCE_RULE;
            plan.summary = "本次任务已完成。" + (evidences.isEmpty()
                    ? "回答暂无可追溯的引用来源，反馈由本地规则生成，仅供参考。"
                    : "回答引用了 " + evidences.size() + " 条资料，反馈由本地规则生成，仅供参考。");
            plan.nextStep = "先复习薄弱点，再独立完成推荐练习。";
        }
        if (plan.weakPoints.isEmpty()) {
            plan.weakPoints = fallbackWeakPoints(query, answer, evidences);
        }
        if (plan.recommendations.isEmpty()) {
            plan.recommendations = fallbackRecommendations(query, evidences);
        }
        return plan;
    }

    /** 无反思事件或反思未给出薄弱点时，基于真实回答/引用/提问生成。 */
    private List<String> fallbackWeakPoints(String query, String answer, List<AiEvidence> evidences) {
        List<String> points = new ArrayList<>();
        if (evidences.isEmpty()) {
            points.add("回答没有可追溯的知识来源，建议结合教材核对关键结论");
        }
        if (StringUtils.isBlank(answer)) {
            points.add("本次没有生成有效回答，建议重新提问或换一种问法");
        }
        if (points.isEmpty()) {
            points.add("围绕「" + abbreviate(query, 40) + "」做一次自测，确认能否独立复述结论");
        }
        return limit(points);
    }

    private List<String> fallbackRecommendations(String query, List<AiEvidence> evidences) {
        List<String> items = new ArrayList<>();
        String topic = abbreviate(StringUtils.defaultIfBlank(query, "本次知识点"), 40);
        items.add("完成 1 道与「" + topic + "」同类型的基础练习");
        items.add("用一句话复述本题关键结论并说明理由");
        if (!evidences.isEmpty() && StringUtils.isNotBlank(evidences.get(0).getTitle())) {
            items.add("阅读引用资料《" + abbreviate(evidences.get(0).getTitle(), 30) + "》的相关章节");
        }
        return limit(items);
    }

    private Reflection loadReflection(String taskId) {
        AiEvent event = eventMapper.findLatestByTaskAndType(taskId, LingXiEventType.VALIDATION_FINISHED);
        if (event == null || StringUtils.isBlank(event.getPayloadJson())) {
            return null;
        }
        try {
            JsonNode payload = objectMapper.readTree(event.getPayloadJson());
            Reflection reflection = new Reflection();
            reflection.rawPayload = event.getPayloadJson();
            reflection.conclusion = firstText(payload, "conclusion", "summary", "result");
            reflection.nextStep = firstText(payload, "nextStep", "next_step", "advice");
            reflection.suggestions = stringList(payload.get("suggestions"));
            reflection.weakPoints = parseChecks(payload.get("checks"));
            return reflection;
        } catch (Exception exception) {
            logger.warn("Failed to parse validation_finished payload for task {}", taskId, exception);
            return null;
        }
    }

    private List<String> parseChecks(JsonNode checks) {
        List<String> weak = new ArrayList<>();
        if (checks == null || !checks.isArray()) {
            return weak;
        }
        for (JsonNode check : checks) {
            if (check == null || check.isTextual()) {
                continue;
            }
            boolean passed = !check.has("passed") || check.path("passed").asBoolean(true);
            boolean failed = !passed || isFailureStatus(check.path("status").asText(""));
            if (!failed) {
                continue;
            }
            String name = firstText(check, "name", "title", "check", "item", "key");
            String detail = firstText(check, "detail", "description", "message", "reason");
            String text = StringUtils.isBlank(name) ? detail
                    : (StringUtils.isBlank(detail) ? name : name + "：" + detail);
            if (StringUtils.isNotBlank(text)) {
                weak.add(text);
            }
        }
        return weak;
    }

    private boolean isFailureStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return "fail".equals(normalized) || "failed".equals(normalized)
                || "error".equals(normalized) || "false".equals(normalized)
                || "ng".equals(normalized);
    }

    private String extractQuery(String requestJson) {
        if (StringUtils.isBlank(requestJson)) {
            return null;
        }
        try {
            return firstText(objectMapper.readTree(requestJson), "query", "question");
        } catch (Exception exception) {
            return null;
        }
    }

    private AiFeedbackView toView(AiFeedback feedback) {
        AiFeedbackView view = new AiFeedbackView();
        view.setTaskId(feedback.getTaskId());
        view.setConversationId(feedback.getConversationId());
        view.setSummary(feedback.getSummary());
        view.setWeakPoints(readList(feedback.getWeakPointsJson()));
        view.setRecommendations(readList(feedback.getRecommendationsJson()));
        view.setNextStep(feedback.getNextStep());
        view.setSource(feedback.getSource());
        view.setCreatedAt(feedback.getCreatedAt());
        return view;
    }

    private List<String> readList(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return stringList(node);
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item != null && item.isValueNode() && StringUtils.isNotBlank(item.asText())) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private List<String> limit(List<String> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        if (items.size() <= MAX_ITEMS) {
            return new ArrayList<>(items);
        }
        return new ArrayList<>(items.subList(0, MAX_ITEMS));
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && StringUtils.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    private String abbreviate(String value, int maxWidth) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return StringUtils.abbreviate(value, maxWidth);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private static final class FeedbackPlan {
        private String summary;
        private List<String> weakPoints = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private String nextStep;
        private String source;
    }

    private static final class Reflection {
        private String conclusion;
        private String nextStep;
        private String rawPayload;
        private List<String> weakPoints = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
    }
}
