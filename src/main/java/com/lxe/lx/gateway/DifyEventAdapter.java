package com.lxe.lx.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DifyEventAdapter {

    private final ObjectMapper objectMapper;

    public DifyEventAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Context createContext(String taskId, String conversationId) {
        return new Context(taskId, StringUtils.defaultString(conversationId));
    }

    public LingXiEvent taskStarted(Context context, String query) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("questionSummary", StringUtils.abbreviate(query, 200));
        return event(context, LingXiEventType.TASK_STARTED, "RUNNING", payload);
    }

    public LingXiEvent adapt(Context context, JsonNode source) {
        if (source == null || !source.isObject()) {
            return null;
        }
        updateConversationId(context, source);
        String sourceEvent = source.path("event").asText();
        if ("message".equals(sourceEvent) || "agent_message".equals(sourceEvent)) {
            return answerDelta(context, source);
        }
        if ("node_started".equals(sourceEvent) || "node_finished".equals(sourceEvent)) {
            return nodeProgress(context, source, sourceEvent);
        }
        if ("message_end".equals(sourceEvent)) {
            return taskFinished(context, source);
        }
        if ("workflow_finished".equals(sourceEvent)) {
            return workflowFinished(context, source);
        }
        if ("error".equals(sourceEvent)) {
            return taskError(context, source);
        }
        // Dify may add events independently of LingXiEvent v1. Unknown events are ignored.
        return null;
    }

    public LingXiEvent streamCompleted(Context context) {
        if (!context.terminal.compareAndSet(false, true)) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("finishReason", "stream_completed");
        return event(context, LingXiEventType.TASK_FINISHED, "SUCCEEDED", payload);
    }

    public LingXiEvent streamError(Context context, String code, String message, boolean retryable) {
        if (!context.terminal.compareAndSet(false, true)) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", StringUtils.defaultIfBlank(code, "DIFY_STREAM_ERROR"));
        payload.put("message", StringUtils.defaultIfBlank(message, "AI 流式任务执行失败"));
        payload.put("retryable", retryable);
        return event(context, LingXiEventType.TASK_ERROR, "FAILED", payload);
    }

    private LingXiEvent answerDelta(Context context, JsonNode source) {
        String delta = source.path("answer").asText();
        if (StringUtils.isEmpty(delta)) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("delta", delta);
        putText(payload, "messageId", source.path("message_id"));
        putText(payload, "difyTaskId", source.path("task_id"));
        return event(context, LingXiEventType.ANSWER_DELTA, "RUNNING", payload);
    }

    private LingXiEvent nodeProgress(Context context, JsonNode source, String sourceEvent) {
        JsonNode data = source.path("data");
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, "nodeId", data.path("node_id"));
        putText(payload, "nodeName", data.path("title"));
        putText(payload, "nodeType", data.path("node_type"));
        String defaultStatus = "node_started".equals(sourceEvent) ? "RUNNING" : "SUCCEEDED";
        payload.put("nodeStatus", StringUtils.defaultIfBlank(data.path("status").asText(), defaultStatus));
        if (data.has("elapsed_time") && data.path("elapsed_time").isNumber()) {
            payload.put("elapsedMs", Math.round(data.path("elapsed_time").asDouble() * 1000));
        }
        putText(payload, "difyTaskId", source.path("task_id"));
        return event(context, LingXiEventType.NODE_PROGRESS, "RUNNING", payload);
    }

    private LingXiEvent taskFinished(Context context, JsonNode source) {
        if (!context.terminal.compareAndSet(false, true)) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, "messageId", source.path("message_id"));
        putText(payload, "difyTaskId", source.path("task_id"));
        JsonNode usage = source.path("metadata").path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            payload.put("usage", objectMapper.convertValue(usage, Object.class));
        }
        payload.put("finishReason", "message_end");
        return event(context, LingXiEventType.TASK_FINISHED, "SUCCEEDED", payload);
    }

    private LingXiEvent taskError(Context context, JsonNode source) {
        int status = source.path("status").asInt(0);
        boolean retryable = status == 429 || status >= 500;
        return streamError(
                context,
                source.path("code").asText(),
                source.path("message").asText(),
                retryable
        );
    }

    private LingXiEvent workflowFinished(Context context, JsonNode source) {
        if (!context.terminal.compareAndSet(false, true)) {
            return null;
        }
        JsonNode data = source.path("data");
        String workflowStatus = data.path("status").asText();
        boolean succeeded = StringUtils.isBlank(workflowStatus) || "succeeded".equalsIgnoreCase(workflowStatus);
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, "difyTaskId", source.path("task_id"));
        if (data.has("outputs") && !data.path("outputs").isNull()) {
            payload.put("outputs", objectMapper.convertValue(data.path("outputs"), Object.class));
        }
        payload.put("finishReason", "workflow_finished");
        if (!succeeded) {
            payload.put("code", "DIFY_WORKFLOW_FAILED");
            payload.put("message", StringUtils.defaultIfBlank(data.path("error").asText(), "Workflow 执行失败"));
            payload.put("retryable", false);
            return event(context, LingXiEventType.TASK_ERROR, "FAILED", payload);
        }
        return event(context, LingXiEventType.TASK_FINISHED, "SUCCEEDED", payload);
    }

    private LingXiEvent event(
            Context context,
            String eventType,
            String status,
            Map<String, Object> payload) {
        long sequence = context.sequence.incrementAndGet();
        LingXiEvent event = new LingXiEvent();
        event.setEventId(context.taskId + ":" + sequence);
        event.setSequence(sequence);
        event.setEventType(eventType);
        event.setTaskId(context.taskId);
        event.setConversationId(context.conversationId);
        event.setOccurredAt(Instant.now().toString());
        event.setStatus(status);
        event.setPayload(payload);
        return event;
    }

    private void updateConversationId(Context context, JsonNode source) {
        String conversationId = source.path("conversation_id").asText();
        if (StringUtils.isNotBlank(conversationId)) {
            context.conversationId = conversationId;
        }
    }

    private void putText(Map<String, Object> payload, String key, JsonNode value) {
        if (value != null && value.isValueNode() && StringUtils.isNotBlank(value.asText())) {
            payload.put(key, value.asText());
        }
    }

    public static final class Context {
        private final String taskId;
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private volatile String conversationId;

        private Context(String taskId, String conversationId) {
            this.taskId = taskId;
            this.conversationId = conversationId;
        }

        public String taskId() {
            return taskId;
        }
    }
}
