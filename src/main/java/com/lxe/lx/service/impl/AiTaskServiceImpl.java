package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import com.lxe.lx.domain.dto.AiTaskCreateRequest;
import com.lxe.lx.domain.dto.AiTaskCreateResponse;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.gateway.AiAgentRouter;
import com.lxe.lx.gateway.DifyEventAdapter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStream;
import com.lxe.lx.gateway.DifyStreamListener;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiTaskExecutionService;
import com.lxe.lx.service.AiMessageService;
import com.lxe.lx.service.AiConversationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import com.lxe.lx.service.AiTaskService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Collections;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AiTaskServiceImpl implements AiTaskService {

    private final DifyGateway difyGateway;
    private final AiAgentRouter agentRouter;
    private final DifyEventAdapter eventAdapter;
    private final TaskScheduler taskScheduler;
    private final long sseTimeoutMs;
    private final long heartbeatMs;
    private final AiTaskMapper aiTaskMapper;
    private final AiSubtaskMapper aiSubtaskMapper;
    private final AiTaskExecutionService executionService;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;
    private final AiMessageService messageService;
    private final AiConversationService conversationService;

    public AiTaskServiceImpl(
            DifyGateway difyGateway,
            AiAgentRouter agentRouter,
            DifyEventAdapter eventAdapter,
            @Qualifier("aiStreamTaskScheduler") TaskScheduler taskScheduler,
            AiTaskMapper aiTaskMapper,
            AiSubtaskMapper aiSubtaskMapper,
            AiTaskExecutionService executionService,
            @Qualifier("aiTaskExecutor") TaskExecutor taskExecutor,
            ObjectMapper objectMapper,
            AiMessageService messageService,
            AiConversationService conversationService,
            @Value("${ai.sse.timeout-ms:600000}") long sseTimeoutMs,
            @Value("${ai.sse.heartbeat-ms:15000}") long heartbeatMs) {
        this.difyGateway = difyGateway;
        this.agentRouter = agentRouter;
        this.eventAdapter = eventAdapter;
        this.taskScheduler = taskScheduler;
        this.sseTimeoutMs = sseTimeoutMs;
        this.heartbeatMs = heartbeatMs;
        this.aiTaskMapper = aiTaskMapper;
        this.aiSubtaskMapper = aiSubtaskMapper;
        this.executionService = executionService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @Override
    public AiTaskResponse sendTask(AiTaskRequest request, String userId) {
        long startMs = System.currentTimeMillis();

        DifyChatflowRequest difyRequest = new DifyChatflowRequest();
        difyRequest.setQuery(request.getQuery());
        difyRequest.setConversationId(request.getConversationId());

        JsonNode result = difyGateway.sendChatMessage(
                agentRouter.route(request.getQuery()),
                difyRequest,
                userId
        );

        long elapsedMs = System.currentTimeMillis() - startMs;

        AiTaskResponse response = new AiTaskResponse();
        response.setAnswer(result.has("answer") ? result.get("answer").asText() : "");
        response.setConversationId(result.has("conversation_id") ? result.get("conversation_id").asText() : "");
        response.setMessageId(result.has("message_id") ? result.get("message_id").asText() : "");
        response.setElapsedMs(elapsedMs);
        return response;
    }

    @Override
    public SseEmitter streamTask(AiTaskRequest request, String userId) {
        DifyChatflowRequest difyRequest = new DifyChatflowRequest();
        difyRequest.setQuery(request.getQuery());
        difyRequest.setConversationId(request.getConversationId());

        DifyEventAdapter.Context eventContext = eventAdapter.createContext(
                UUID.randomUUID().toString(),
                request.getConversationId()
        );
        AiSseSession session = new AiSseSession(sseTimeoutMs, heartbeatMs, taskScheduler);
        session.send(eventAdapter.taskStarted(eventContext, request.getQuery()));
        session.startHeartbeat();

        try {
            DifyStream stream = difyGateway.streamChatMessage(
                    agentRouter.route(request.getQuery()),
                    difyRequest,
                    userId,
                    new DifyStreamListener() {
                        @Override
                        public void onEvent(JsonNode sourceEvent) {
                            LingXiEvent event = eventAdapter.adapt(eventContext, sourceEvent);
                            if (event != null) {
                                session.send(event);
                            }
                        }

                        @Override
                        public void onComplete() {
                            LingXiEvent event = eventAdapter.streamCompleted(eventContext);
                            if (event != null) {
                                session.send(event);
                            }
                            session.complete();
                        }

                        @Override
                        public void onError(DifyGatewayException exception) {
                            LingXiEvent event = eventAdapter.streamError(
                                    eventContext,
                                    "DIFY_STREAM_ERROR",
                                    exception.getMessage(),
                                    exception.isRetryable()
                            );
                            if (event != null) {
                                session.send(event);
                            }
                            session.complete();
                        }
                    }
            );
            session.attach(stream);
        } catch (DifyGatewayException exception) {
            LingXiEvent event = eventAdapter.streamError(
                    eventContext,
                    "DIFY_STREAM_SETUP_ERROR",
                    exception.getMessage(),
                    exception.isRetryable()
            );
            if (event != null) {
                session.send(event);
            }
            session.complete();
        }
        return session.getEmitter();
    }

    @Override
    @Transactional
    public AiTaskCreateResponse createTask(AiTaskCreateRequest request, String userId) {
        if (request == null) {
            throw new IllegalArgumentException("AI 任务请求不能为空");
        }
        String taskType = request.getTaskType() == null ? "CHATFLOW" : request.getTaskType().toUpperCase();
        if (!"CHATFLOW".equals(taskType) && !"WORKFLOW".equals(taskType)) {
            throw new IllegalArgumentException("taskType 只能是 CHATFLOW 或 WORKFLOW");
        }
        if ("CHATFLOW".equals(taskType) && StringUtils.isBlank(request.getQuery())) {
            throw new IllegalArgumentException("Chatflow query 不能为空");
        }
        if ("WORKFLOW".equals(taskType)
                && (request.getInputs() == null || request.getInputs().isEmpty())) {
            throw new IllegalArgumentException("Workflow inputs 不能为空");
        }
        String conversationId = request.getConversationId();
        String difyConversationId = null;
        if (StringUtils.isBlank(conversationId)) {
            conversationId = uuid();
        } else {
            AiTask conversation = aiTaskMapper.findLatestByConversationId(conversationId);
            if (conversation == null || !userId.equals(conversation.getUserId())) {
                throw new IllegalArgumentException("conversationId 不属于当前用户");
            }
            difyConversationId = conversation.getDifyConversationId();
        }
        request.setTaskType(taskType);
        conversationService.createIfAbsent(conversationId, userId,
                StringUtils.defaultIfBlank(request.getQuery(), "Workflow 会话"));
        AiTask task = new AiTask();
        task.setId(uuid());
        task.setUserId(userId);
        task.setConversationId(conversationId);
        task.setTaskType(taskType);
        task.setDifyConversationId(difyConversationId);
        task.setStatus("CREATED");
        task.setProgress(0);
        task.setRequestJson(writeJson(request));
        task.setVersion(1);
        task.setEventSequence(0L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCreatedAt());
        aiTaskMapper.insert(task);
        if ("CHATFLOW".equals(taskType) && StringUtils.isNotBlank(request.getQuery())) {
            messageService.saveUserQuestion(conversationId, task.getId(), request.getQuery());
        } else if ("WORKFLOW".equals(taskType) && StringUtils.isNotBlank(request.getQuery())) {
            messageService.saveUserQuestion(conversationId, task.getId(), request.getQuery());
        }

        AiSubtask subtask = new AiSubtask();
        subtask.setId(uuid());
        subtask.setTaskId(task.getId());
        subtask.setAgentType(taskType);
        subtask.setGoal(StringUtils.defaultIfBlank(request.getQuery(), "执行 Workflow"));
        subtask.setInputsJson(writeJson(request.getInputs() == null ? Collections.emptyMap() : request.getInputs()));
        subtask.setDependencyJson("[]");
        subtask.setStatus("CREATED");
        subtask.setExecutionNo(1);
        subtask.setRetryCount(0);
        subtask.setVersion(1);
        subtask.setCreatedAt(task.getCreatedAt());
        subtask.setUpdatedAt(task.getCreatedAt());
        aiSubtaskMapper.insert(subtask);

        final String createdTaskId = task.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> executionService.execute(createdTaskId));
            }
        });
        AiTaskCreateResponse response = new AiTaskCreateResponse();
        response.setTaskId(task.getId());
        response.setConversationId(conversationId);
        response.setStatus(task.getStatus());
        response.setEventUrl("/api/ai/tasks/" + task.getId() + "/events");
        return response;
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI 任务请求序列化失败", exception);
        }
    }
}
