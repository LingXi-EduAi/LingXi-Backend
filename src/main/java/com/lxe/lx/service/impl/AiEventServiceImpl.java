package com.lxe.lx.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.AiTaskStatus;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.TaskEventBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class AiEventServiceImpl implements AiEventService {
    private final AiTaskMapper taskMapper;
    private final AiSubtaskMapper subtaskMapper;
    private final AiEventMapper eventMapper;
    private final TaskEventBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    public AiEventServiceImpl(
            AiTaskMapper taskMapper,
            AiSubtaskMapper subtaskMapper,
            AiEventMapper eventMapper,
            TaskEventBroadcaster broadcaster,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.subtaskMapper = subtaskMapper;
        this.eventMapper = eventMapper;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AiEvent record(
            String taskId,
            String subtaskId,
            LingXiEvent contract,
            String sourceEventId,
            String resultJson,
            String errorCode,
            String errorMessage) {
        AiTask task = taskMapper.lockById(taskId);
        if (task == null) {
            throw new IllegalStateException("AI task does not exist: " + taskId);
        }
        if (sourceEventId != null) {
            AiEvent duplicate = eventMapper.findBySourceEventId(taskId, sourceEventId);
            if (duplicate != null) {
                return duplicate;
            }
        }
        String nextStatus = contract.getStatus();
        if (AiTaskStatus.isTerminal(task.getStatus())) {
            return null;
        }
        if (!task.getStatus().equals(nextStatus)
                && !AiTaskStatus.canTransition(task.getStatus(), nextStatus)) {
            throw new IllegalStateException("Invalid AI task status transition: "
                    + task.getStatus() + " -> " + nextStatus);
        }

        long sequence = task.getEventSequence() + 1;
        LocalDateTime now = LocalDateTime.now();
        contract.setTaskId(taskId);
        contract.setConversationId(task.getConversationId());
        contract.setSequence(sequence);
        contract.setEventId(taskId + ":" + sequence);
        contract.setOccurredAt(now.toInstant(ZoneOffset.ofHours(8)).toString());

        AiEvent event = new AiEvent();
        event.setId(uuid());
        event.setTaskId(taskId);
        event.setSubtaskId(subtaskId);
        event.setSequence(sequence);
        event.setEventType(contract.getEventType());
        event.setStatus(nextStatus);
        event.setPayloadVersion(contract.getPayloadVersion());
        event.setPayloadJson(writeJson(contract.getPayload()));
        event.setSourceEventId(sourceEventId);
        event.setOccurredAt(now);
        eventMapper.insert(event);

        task.setEventSequence(sequence);
        task.setStatus(nextStatus);
        task.setProgress(progress(task, contract));
        task.setResultJson(resultJson);
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        task.setUpdatedAt(now);
        if (AiTaskStatus.RUNNING.equals(nextStatus) && task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        if (AiTaskStatus.isTerminal(nextStatus)) {
            task.setFinishedAt(now);
        }
        if (taskMapper.updateAfterEvent(task) != 1) {
            throw new IllegalStateException("AI task optimistic lock conflict: " + taskId);
        }
        updateSubtask(subtaskId, taskId, nextStatus, errorCode, errorMessage, now);

        LingXiEvent published = toContract(event, task.getConversationId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcaster.publish(published);
            }
        });
        return event;
    }

    @Override
    public LingXiEvent toContract(AiEvent event, String conversationId) {
        LingXiEvent contract = new LingXiEvent();
        contract.setEventId(event.getTaskId() + ":" + event.getSequence());
        contract.setSequence(event.getSequence());
        contract.setEventType(event.getEventType());
        contract.setTaskId(event.getTaskId());
        contract.setConversationId(conversationId);
        contract.setOccurredAt(event.getOccurredAt().toInstant(ZoneOffset.ofHours(8)).toString());
        contract.setStatus(event.getStatus());
        contract.setPayloadVersion(event.getPayloadVersion());
        try {
            contract.setPayload(objectMapper.readValue(
                    event.getPayloadJson(), new TypeReference<Map<String, Object>>() { }));
        } catch (JsonProcessingException exception) {
            contract.setPayload(Collections.emptyMap());
        }
        return contract;
    }

    private void updateSubtask(
            String subtaskId,
            String taskId,
            String taskStatus,
            String errorCode,
            String errorMessage,
            LocalDateTime now) {
        if (subtaskId == null) {
            return;
        }
        AiSubtask subtask = subtaskMapper.findByIdAndTask(subtaskId, taskId);
        if (subtask == null) {
            return;
        }
        subtask.setStatus(taskStatus);
        subtask.setErrorCode(errorCode);
        subtask.setErrorMessage(errorMessage);
        subtask.setUpdatedAt(now);
        if (AiTaskStatus.RUNNING.equals(taskStatus) && subtask.getStartedAt() == null) {
            subtask.setStartedAt(now);
        }
        if (AiTaskStatus.isTerminal(taskStatus)) {
            subtask.setFinishedAt(now);
        }
        if (subtaskMapper.updateStatus(subtask) != 1) {
            throw new IllegalStateException("AI subtask optimistic lock conflict: " + subtaskId);
        }
    }

    private int progress(AiTask task, LingXiEvent event) {
        if (AiTaskStatus.SUCCEEDED.equals(event.getStatus())) {
            return 100;
        }
        if (LingXiEventType.TASK_STARTED.equals(event.getEventType())) {
            return Math.max(task.getProgress(), 1);
        }
        if (LingXiEventType.NODE_PROGRESS.equals(event.getEventType())) {
            return Math.min(95, Math.max(task.getProgress(), 10) + 5);
        }
        return task.getProgress();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot serialize AI event payload", exception);
        }
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
