package com.lxe.lx.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.AiTaskStatus;
import com.lxe.lx.domain.dto.AiSubtaskSnapshot;
import com.lxe.lx.domain.dto.AiTaskSnapshot;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiRuntimeRegistry;
import com.lxe.lx.service.AiTaskApiException;
import com.lxe.lx.service.AiTaskControlService;
import com.lxe.lx.service.AiTaskExecutionService;
import com.lxe.lx.service.OrderedEventBuffer;
import com.lxe.lx.service.TaskEventBroadcaster;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTaskControlServiceImpl implements AiTaskControlService {
    private static final Logger logger = LogManager.getLogger(AiTaskControlServiceImpl.class);

    private final AiTaskMapper taskMapper;
    private final AiSubtaskMapper subtaskMapper;
    private final AiEventMapper eventMapper;
    private final AiEventService eventService;
    private final TaskEventBroadcaster broadcaster;
    private final AiRuntimeRegistry runtimeRegistry;
    private final DifyGateway difyGateway;
    private final AiTaskExecutionService executionService;
    private final TaskExecutor taskExecutor;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final long sseTimeoutMs;
    private final long heartbeatMs;

    public AiTaskControlServiceImpl(
            AiTaskMapper taskMapper,
            AiSubtaskMapper subtaskMapper,
            AiEventMapper eventMapper,
            AiEventService eventService,
            TaskEventBroadcaster broadcaster,
            AiRuntimeRegistry runtimeRegistry,
            DifyGateway difyGateway,
            AiTaskExecutionService executionService,
            @Qualifier("aiTaskExecutor") TaskExecutor taskExecutor,
            @Qualifier("aiStreamTaskScheduler") TaskScheduler taskScheduler,
            ObjectMapper objectMapper,
            @Value("${ai.subtask.max-retries:3}") int maxRetries,
            @Value("${ai.sse.timeout-ms:600000}") long sseTimeoutMs,
            @Value("${ai.sse.heartbeat-ms:15000}") long heartbeatMs) {
        this.taskMapper = taskMapper;
        this.subtaskMapper = subtaskMapper;
        this.eventMapper = eventMapper;
        this.eventService = eventService;
        this.broadcaster = broadcaster;
        this.runtimeRegistry = runtimeRegistry;
        this.difyGateway = difyGateway;
        this.executionService = executionService;
        this.taskExecutor = taskExecutor;
        this.taskScheduler = taskScheduler;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
        this.sseTimeoutMs = sseTimeoutMs;
        this.heartbeatMs = heartbeatMs;
    }

    @Override
    public AiTaskSnapshot getSnapshot(String taskId, String userId) {
        AiTask task = ownedTask(taskId, userId);
        return snapshot(task, subtaskMapper.findByTaskId(taskId));
    }

    @Override
    public SseEmitter subscribe(String taskId, String userId, String lastEventId) {
        AiTask task = ownedTask(taskId, userId);
        long lastSequence = parseLastEventId(taskId, lastEventId);
        if (lastSequence > task.getEventSequence()) {
            throw badRequest("Last-Event-ID sequence 超出任务当前事件范围");
        }

        AiSseSession session = new AiSseSession(sseTimeoutMs, heartbeatMs, taskScheduler);
        OrderedEventBuffer handoff = new OrderedEventBuffer(lastSequence, event -> {
            session.send(event);
            if (AiTaskStatus.isTerminal(event.getStatus())) {
                session.complete();
            }
        });
        TaskEventBroadcaster.Subscription subscription = broadcaster.subscribe(taskId, handoff::addLive);
        session.attach(subscription::close);
        session.startHeartbeat();
        for (AiEvent event : eventMapper.findAfterSequence(taskId, lastSequence)) {
            handoff.addReplay(eventService.toContract(event, task.getConversationId()));
        }
        handoff.finishReplay();
        if (AiTaskStatus.isTerminal(task.getStatus()) && lastSequence == task.getEventSequence()) {
            session.complete();
        }
        return session.getEmitter();
    }

    @Override
    @Transactional
    public AiTaskSnapshot stop(String taskId, String userId) {
        AiTask task = lockOwnedTask(taskId, userId);
        if (AiTaskStatus.STOPPED.equals(task.getStatus())) {
            return snapshot(task, subtaskMapper.findByTaskId(taskId));
        }
        if (!AiTaskStatus.RUNNING.equals(task.getStatus())) {
            throw conflict("只有 RUNNING 任务可以停止");
        }
        AiSubtask subtask = firstSubtask(taskId);
        LingXiEvent event = new LingXiEvent();
        event.setEventType(LingXiEventType.TASK_FINISHED);
        event.setStatus(AiTaskStatus.STOPPED);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("finishReason", "user_stopped");
        event.setPayload(payload);
        eventService.record(taskId, subtask == null ? null : subtask.getId(), event,
                "lingxi:user_stopped", null, null, null);

        afterCommit(() -> {
            runtimeRegistry.cancel(taskId);
            stopDifyBestEffort(task);
        });
        return snapshot(taskMapper.findByIdAndUser(taskId, userId), subtaskMapper.findByTaskId(taskId));
    }

    @Override
    @Transactional
    public AiTaskSnapshot retry(String taskId, String subtaskId, String userId) {
        AiTask task = lockOwnedTask(taskId, userId);
        AiSubtask subtask = subtaskMapper.lockByIdAndTask(subtaskId, taskId);
        if (subtask == null) {
            throw notFound();
        }
        if (!AiTaskStatus.FAILED.equals(subtask.getStatus())) {
            throw conflict("只有 FAILED 子任务可以重试");
        }
        if (!AiTaskStatus.FAILED.equals(task.getStatus())
                && !AiTaskStatus.PARTIAL_SUCCESS.equals(task.getStatus())) {
            throw conflict("当前任务状态不允许重试子任务");
        }
        if (subtask.getRetryCount() >= maxRetries) {
            throw conflict("子任务已达到最大重试次数");
        }
        if (!"CHATFLOW".equals(subtask.getAgentType()) && !"WORKFLOW".equals(subtask.getAgentType())) {
            throw conflict("该 Agent 类型暂不支持重试");
        }
        requireDependenciesSucceeded(subtask, subtaskMapper.findByTaskId(taskId));

        LocalDateTime now = LocalDateTime.now();
        subtask.setExecutionNo(subtask.getExecutionNo() + 1);
        subtask.setRetryCount(subtask.getRetryCount() + 1);
        subtask.setStartedAt(now);
        subtask.setUpdatedAt(now);
        if (subtaskMapper.prepareRetry(subtask) != 1) {
            throw conflict("子任务正在被其他请求重试");
        }
        task.setUpdatedAt(now);
        if (taskMapper.restartForRetry(task) != 1) {
            throw conflict("任务状态已发生变化");
        }

        LingXiEvent assigned = new LingXiEvent();
        assigned.setEventType(LingXiEventType.AGENT_ASSIGNED);
        assigned.setStatus(AiTaskStatus.RUNNING);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentType", subtask.getAgentType());
        payload.put("executionNo", subtask.getExecutionNo());
        payload.put("retryCount", subtask.getRetryCount());
        assigned.setPayload(payload);
        eventService.record(taskId, subtaskId, assigned,
                "lingxi:retry:" + subtaskId + ":" + subtask.getExecutionNo(),
                null, null, null);

        afterCommit(() -> taskExecutor.execute(() -> executionService.retrySubtask(taskId, subtaskId)));
        return snapshot(taskMapper.findByIdAndUser(taskId, userId), subtaskMapper.findByTaskId(taskId));
    }

    private void requireDependenciesSucceeded(AiSubtask target, List<AiSubtask> subtasks) {
        List<String> dependencies;
        try {
            dependencies = objectMapper.readValue(
                    StringUtils.defaultIfBlank(target.getDependencyJson(), "[]"),
                    new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw conflict("子任务依赖配置无效");
        }
        Map<String, String> statuses = new HashMap<>();
        for (AiSubtask subtask : subtasks) {
            statuses.put(subtask.getId(), subtask.getStatus());
        }
        for (String dependency : dependencies) {
            if (!AiTaskStatus.SUCCEEDED.equals(statuses.get(dependency))) {
                throw conflict("子任务依赖尚未完成");
            }
        }
    }

    private long parseLastEventId(String taskId, String lastEventId) {
        if (StringUtils.isBlank(lastEventId)) {
            return 0;
        }
        int separator = lastEventId.lastIndexOf(':');
        if (separator <= 0 || !taskId.equals(lastEventId.substring(0, separator))) {
            throw badRequest("Last-Event-ID 格式错误或 taskId 不匹配");
        }
        try {
            long sequence = Long.parseLong(lastEventId.substring(separator + 1));
            if (sequence < 0) {
                throw new NumberFormatException();
            }
            return sequence;
        } catch (NumberFormatException exception) {
            throw badRequest("Last-Event-ID sequence 必须是非负整数");
        }
    }

    private AiTask ownedTask(String taskId, String userId) {
        AiTask task = taskMapper.findByIdAndUser(taskId, userId);
        if (task == null) {
            throw notFound();
        }
        return task;
    }

    private AiTask lockOwnedTask(String taskId, String userId) {
        AiTask task = taskMapper.lockById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw notFound();
        }
        return task;
    }

    private AiSubtask firstSubtask(String taskId) {
        List<AiSubtask> subtasks = subtaskMapper.findByTaskId(taskId);
        return subtasks.isEmpty() ? null : subtasks.get(0);
    }

    private AiTaskSnapshot snapshot(AiTask task, List<AiSubtask> subtasks) {
        AiTaskSnapshot result = new AiTaskSnapshot();
        result.setTaskId(task.getId());
        result.setConversationId(task.getConversationId());
        result.setTaskType(task.getTaskType());
        result.setStatus(task.getStatus());
        result.setProgress(task.getProgress());
        result.setResult(readJson(task.getResultJson()));
        result.setErrorCode(task.getErrorCode());
        result.setErrorMessage(task.getErrorMessage());
        List<AiSubtaskSnapshot> children = new ArrayList<>();
        for (AiSubtask subtask : subtasks) {
            AiSubtaskSnapshot child = new AiSubtaskSnapshot();
            child.setSubtaskId(subtask.getId());
            child.setParentId(subtask.getParentId());
            child.setAgentType(subtask.getAgentType());
            child.setGoal(subtask.getGoal());
            child.setStatus(subtask.getStatus());
            child.setExecutionNo(subtask.getExecutionNo());
            child.setRetryCount(subtask.getRetryCount());
            child.setErrorCode(subtask.getErrorCode());
            child.setErrorMessage(subtask.getErrorMessage());
            children.add(child);
        }
        result.setSubtasks(children);
        return result;
    }

    private Object readJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void stopDifyBestEffort(AiTask task) {
        if (StringUtils.isBlank(task.getDifyTaskId())) {
            return;
        }
        try {
            if ("WORKFLOW".equals(task.getTaskType())) {
                difyGateway.stopWorkflow(task.getDifyTaskId(), task.getUserId());
            } else {
                difyGateway.stopChatMessage(DifyChatApplication.CHATFLOW,
                        task.getDifyTaskId(), task.getUserId());
            }
        } catch (RuntimeException exception) {
            logger.warn("Best-effort Dify stop failed for task {}", task.getId());
        }
    }

    private AiTaskApiException notFound() {
        return new AiTaskApiException(404, "AI 任务不存在");
    }

    private AiTaskApiException badRequest(String message) {
        return new AiTaskApiException(400, message);
    }

    private AiTaskApiException conflict(String message) {
        return new AiTaskApiException(409, message);
    }
}
