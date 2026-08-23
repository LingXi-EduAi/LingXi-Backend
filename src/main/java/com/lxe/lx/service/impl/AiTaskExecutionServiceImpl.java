package com.lxe.lx.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.AiTaskStatus;
import com.lxe.lx.domain.dto.AiTaskCreateRequest;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.gateway.AiAgentRouter;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyEventAdapter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStream;
import com.lxe.lx.gateway.DifyStreamListener;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiRuntimeRegistry;
import com.lxe.lx.service.AiTaskExecutionService;
import com.lxe.lx.service.AiTaskResultPersistenceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

@Service
public class AiTaskExecutionServiceImpl implements AiTaskExecutionService {
    private final AiTaskMapper taskMapper;
    private final AiSubtaskMapper subtaskMapper;
    private final AiEventService eventService;
    private final DifyGateway difyGateway;
    private final AiAgentRouter agentRouter;
    private final DifyEventAdapter eventAdapter;
    private final AiRuntimeRegistry runtimeRegistry;
    private final ObjectMapper objectMapper;
    private final AiTaskResultPersistenceService resultPersistenceService;

    public AiTaskExecutionServiceImpl(
            AiTaskMapper taskMapper,
            AiSubtaskMapper subtaskMapper,
            AiEventService eventService,
            DifyGateway difyGateway,
            AiAgentRouter agentRouter,
            DifyEventAdapter eventAdapter,
            AiRuntimeRegistry runtimeRegistry,
            ObjectMapper objectMapper,
            AiTaskResultPersistenceService resultPersistenceService) {
        this.taskMapper = taskMapper;
        this.subtaskMapper = subtaskMapper;
        this.eventService = eventService;
        this.difyGateway = difyGateway;
        this.agentRouter = agentRouter;
        this.eventAdapter = eventAdapter;
        this.runtimeRegistry = runtimeRegistry;
        this.objectMapper = objectMapper;
        this.resultPersistenceService = resultPersistenceService;
    }

    @Override
    public void execute(String taskId) {
        AiTask task = taskMapper.findById(taskId);
        if (task == null || !AiTaskStatus.CREATED.equals(task.getStatus())) {
            return;
        }
        AiSubtask subtask = firstSubtask(taskId);
        String subtaskId = subtask == null ? null : subtask.getId();
        AiTaskCreateRequest request;
        try {
            request = objectMapper.readValue(task.getRequestJson(), AiTaskCreateRequest.class);
        } catch (Exception exception) {
            fail(taskId, subtaskId, "INVALID_REQUEST", "AI 任务请求无法读取", exception);
            return;
        }

        DifyEventAdapter.Context context = eventAdapter.createContext(taskId, task.getConversationId());
        try {
            record(subtaskId, taskId, eventAdapter.taskStarted(context, request.getQuery()),
                    "lingxi:task_started", null, null, null);
            record(subtaskId, taskId, simpleEvent(context, LingXiEventType.TASK_DECOMPOSED, "RUNNING",
                    singleton("taskType", request.getTaskType())), "lingxi:task_decomposed", null, null, null);
            record(subtaskId, taskId, simpleEvent(context, LingXiEventType.AGENT_ASSIGNED, "RUNNING",
                    singleton("agentType", request.getTaskType())), "lingxi:agent_assigned", null, null, null);
        } catch (RuntimeException exception) {
            fail(taskId, subtaskId, "PERSISTENCE_ERROR", "AI 任务初始化失败", exception);
            return;
        }

        startAgent(task, subtaskId, context, request);
    }

    @Override
    public void retrySubtask(String taskId, String subtaskId) {
        AiTask task = taskMapper.findById(taskId);
        AiSubtask subtask = subtaskMapper.findByIdAndTask(subtaskId, taskId);
        if (task == null || subtask == null
                || !AiTaskStatus.RUNNING.equals(task.getStatus())
                || !AiTaskStatus.RUNNING.equals(subtask.getStatus())) {
            return;
        }
        AiTaskCreateRequest request = new AiTaskCreateRequest();
        request.setTaskType(subtask.getAgentType());
        request.setQuery(subtask.getGoal());
        try {
            request.setInputs(objectMapper.readValue(
                    StringUtils.defaultIfBlank(subtask.getInputsJson(), "{}"),
                    new TypeReference<Map<String, Object>>() { }));
        } catch (Exception exception) {
            fail(taskId, subtaskId, "INVALID_SUBTASK_INPUTS", "子任务输入无法读取", exception);
            return;
        }
        startAgent(task, subtaskId,
                eventAdapter.createContext(taskId, task.getConversationId()), request);
    }

    private void startAgent(AiTask task, String subtaskId,
                            DifyEventAdapter.Context context, AiTaskCreateRequest request) {
        ExecutionListener listener = new ExecutionListener(task, subtaskId, context);
        try {
            DifyStream stream;
            if ("WORKFLOW".equalsIgnoreCase(request.getTaskType())) {
                stream = difyGateway.streamWorkflow(request.getInputs(), task.getUserId(), listener);
            } else {
                DifyChatflowRequest chatRequest = new DifyChatflowRequest();
                chatRequest.setQuery(request.getQuery());
                chatRequest.setInputs(request.getInputs());
                chatRequest.setConversationId(task.getDifyConversationId());
                stream = difyGateway.streamChatMessage(
                        agentRouter.route(request.getQuery()), chatRequest, task.getUserId(), listener);
            }
            runtimeRegistry.register(task.getId(), stream);
            if (AiTaskStatus.isTerminal(taskMapper.findById(task.getId()).getStatus())) {
                runtimeRegistry.remove(task.getId());
            }
        } catch (DifyGatewayException exception) {
            listener.onError(exception);
        } catch (RuntimeException exception) {
            listener.onError(new DifyGatewayException("AI 任务启动失败", null, false, exception));
        }
    }

    private final class ExecutionListener implements DifyStreamListener {
        private final AiTask task;
        private final String subtaskId;
        private final DifyEventAdapter.Context context;
        private final StringBuilder answer = new StringBuilder();
        private Map<String, Object> outputs = Collections.emptyMap();
        private final List<AiEvidence> evidences = new ArrayList<>();
        private final Set<String> evidenceKeys = new HashSet<>();

        private ExecutionListener(AiTask task, String subtaskId,
                                  DifyEventAdapter.Context context) {
            this.task = task;
            this.subtaskId = subtaskId;
            this.context = context;
        }

        @Override
        public void onEvent(JsonNode sourceEvent) {
            updateExternalIds(task.getId(), sourceEvent);
            JsonNode outputNode = sourceEvent.path("data").path("outputs");
            if (outputNode.isObject()) {
                outputs = objectMapper.convertValue(outputNode, Map.class);
            }
            collectEvidence(sourceEvent);
            if (sourceEvent.has("answer")) {
                answer.append(sourceEvent.path("answer").asText());
            }
            LingXiEvent event = eventAdapter.adapt(context, sourceEvent);
            if (event != null) {
                String resultJson = isFinished(event) ? resultJson() : null;
                String errorCode = isError(event) ? text(event.getPayload(), "code") : null;
                String errorMessage = isError(event) ? text(event.getPayload(), "message") : null;
                if (isFinished(event) || isError(event)) {
                    resultPersistenceService.recordTerminalEvent(
                            task.getId(), subtaskId, event, sourceId(sourceEvent),
                            resultJson, errorCode, errorMessage,
                            isError(event) ? null : answerText(sourceEvent),
                            firstText(sourceEvent, "message_id"), evidences);
                } else {
                    record(subtaskId, task.getId(), event, sourceId(sourceEvent),
                            resultJson, errorCode, errorMessage);
                }
                if (isFinished(event) || isError(event)) {
                    runtimeRegistry.remove(task.getId());
                }
            }
        }

        @Override
        public void onComplete() {
            LingXiEvent event = eventAdapter.streamCompleted(context);
            if (event != null) {
                resultPersistenceService.recordTerminalEvent(
                        task.getId(), subtaskId, event, "lingxi:stream_completed",
                        resultJson(), null, null, answer.toString(), null, evidences);
            }
            runtimeRegistry.remove(task.getId());
        }

        @Override
        public void onError(DifyGatewayException exception) {
            LingXiEvent event = eventAdapter.streamError(
                    context, "DIFY_STREAM_ERROR", exception.getMessage(), exception.isRetryable());
            if (event != null) {
                record(subtaskId, task.getId(), event, "lingxi:stream_error:" + UUID.randomUUID(),
                        null, "DIFY_STREAM_ERROR", exception.getMessage());
            }
            runtimeRegistry.remove(task.getId());
        }

        private boolean isFinished(LingXiEvent event) {
            return LingXiEventType.TASK_FINISHED.equals(event.getEventType());
        }

        private boolean isError(LingXiEvent event) {
            return LingXiEventType.TASK_ERROR.equals(event.getEventType());
        }

        private String resultJson() {
            Map<String, Object> result = new LinkedHashMap<>();
            if (answer.length() > 0) {
                result.put("answer", answer.toString());
            }
            if (!outputs.isEmpty()) {
                result.put("outputs", outputs);
            }
            return writeJson(result);
        }

        private String answerText(JsonNode sourceEvent) {
            if (answer.length() > 0) {
                return answer.toString();
            }
            JsonNode outputsNode = sourceEvent.path("data").path("outputs");
            if (!outputsNode.isObject()) {
                return null;
            }
            for (String field : new String[]{"answer", "result", "text", "content"}) {
                JsonNode value = outputsNode.get(field);
                if (value != null && value.isValueNode() && StringUtils.isNotBlank(value.asText())) {
                    return value.asText();
                }
            }
            return null;
        }

        private void collectEvidence(JsonNode sourceEvent) {
            JsonNode resources = sourceEvent.path("metadata").path("retriever_resources");
            if (!resources.isArray()) {
                resources = sourceEvent.path("data").path("retriever_resources");
            }
            if (!resources.isArray()) {
                resources = sourceEvent.path("data").path("retrieval_resources");
            }
            if (!resources.isArray()) {
                return;
            }
            for (JsonNode resource : resources) {
                AiEvidence evidence = evidence(resource);
                if (evidence == null) {
                    continue;
                }
                String key = String.valueOf(evidence.getSourceType()) + "|"
                        + String.valueOf(evidence.getTitle()) + "|"
                        + String.valueOf(evidence.getUrl()) + "|"
                        + String.valueOf(evidence.getContentSnippet());
                if (evidenceKeys.add(key)) {
                    evidences.add(evidence);
                }
            }
        }

        private AiEvidence evidence(JsonNode resource) {
            String snippet = firstText(resource, "content", "content_snippet", "snippet");
            String title = firstText(resource, "document_name", "title", "name");
            String url = firstText(resource, "url", "link");
            String sourceType = firstText(resource, "data_source_type", "source_type", "type");
            JsonNode scoreNode = resource.get("score");
            if (StringUtils.isBlank(snippet) && StringUtils.isBlank(title)
                    && StringUtils.isBlank(url) && scoreNode == null) {
                return null;
            }
            AiEvidence evidence = new AiEvidence();
            evidence.setId(UUID.randomUUID().toString().replace("-", ""));
            evidence.setSourceType(StringUtils.defaultIfBlank(sourceType, "KNOWLEDGE_BASE"));
            evidence.setTitle(title);
            evidence.setUrl(url);
            evidence.setContentSnippet(snippet);
            if (scoreNode != null && scoreNode.isNumber()) {
                evidence.setScore(new BigDecimal(scoreNode.asText()));
            }
            return evidence;
        }
    }

    private void record(String subtaskId, String taskId, LingXiEvent event, String sourceId,
                        String resultJson, String errorCode, String errorMessage) {
        if (event != null) {
            eventService.record(taskId, subtaskId, event, sourceId, resultJson, errorCode, errorMessage);
        }
    }

    private void fail(String taskId, String subtaskId, String code, String message, Exception exception) {
        DifyEventAdapter.Context context = eventAdapter.createContext(taskId, null);
        LingXiEvent event = eventAdapter.streamError(context, code, message, false);
        record(subtaskId, taskId, event, "lingxi:setup_error", null, code, message);
    }

    private AiSubtask firstSubtask(String taskId) {
        java.util.List<AiSubtask> subtasks = subtaskMapper.findByTaskId(taskId);
        return subtasks.isEmpty() ? null : subtasks.get(0);
    }

    private void updateExternalIds(String taskId, JsonNode source) {
        String difyTaskId = firstText(source, "task_id", "workflow_run_id");
        String difyConversationId = firstText(source, "conversation_id");
        if (StringUtils.isNotBlank(difyTaskId) || StringUtils.isNotBlank(difyConversationId)) {
            taskMapper.updateDifyIds(taskId, difyTaskId, difyConversationId);
        }
    }

    private String sourceId(JsonNode source) {
        String eventType = source.path("event").asText();
        // Dify reuses the message identity for every answer chunk; treating it as an
        // event identity would drop later chunks from persisted replay.
        if ("message".equals(eventType) || "agent_message".equals(eventType)) {
            return null;
        }
        String explicitId = firstText(source, "id");
        if (StringUtils.isNotBlank(explicitId)) {
            return eventType + ":" + explicitId;
        }
        if ("node_started".equals(eventType) || "node_finished".equals(eventType)) {
            String executionId = firstText(source.path("data"), "id", "execution_id", "node_execution_id");
            if (StringUtils.isNotBlank(executionId)) {
                return eventType + ":" + executionId;
            }
        }
        if ("message_end".equals(eventType)) {
            String messageId = firstText(source, "message_id");
            return StringUtils.isBlank(messageId) ? null : eventType + ":" + messageId;
        }
        if ("workflow_finished".equals(eventType)) {
            String runId = firstText(source, "workflow_run_id", "task_id");
            return StringUtils.isBlank(runId) ? null : eventType + ":" + runId;
        }
        return null;
    }

    private String firstText(JsonNode source, String... fields) {
        for (String field : fields) {
            if (source.hasNonNull(field) && StringUtils.isNotBlank(source.path(field).asText())) {
                return source.path(field).asText();
            }
        }
        return null;
    }

    private LingXiEvent simpleEvent(DifyEventAdapter.Context context, String type,
                                    String status, Map<String, Object> payload) {
        LingXiEvent event = new LingXiEvent();
        event.setEventType(type);
        event.setStatus(status);
        event.setPayload(payload);
        event.setTaskId(contextTaskId(context));
        return event;
    }

    private String contextTaskId(DifyEventAdapter.Context context) {
        return context.taskId();
    }

    private Map<String, Object> singleton(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private String text(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 任务结果序列化失败", exception);
        }
    }
}
