package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiTaskRecoveryService {
    private static final Logger logger = LogManager.getLogger(AiTaskRecoveryService.class);

    private final AiTaskMapper taskMapper;
    private final AiEventService eventService;
    private final AiSubtaskMapper subtaskMapper;
    private final DifyGateway difyGateway;
    private final boolean enabled;

    public AiTaskRecoveryService(
            AiTaskMapper taskMapper,
            AiSubtaskMapper subtaskMapper,
            AiEventService eventService,
            DifyGateway difyGateway,
            @Value("${ai.task.recovery-enabled:true}") boolean enabled) {
        this.taskMapper = taskMapper;
        this.subtaskMapper = subtaskMapper;
        this.eventService = eventService;
        this.difyGateway = difyGateway;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        if (!enabled) {
            return;
        }
        for (AiTask task : taskMapper.findRunningTasks()) {
            try {
                LingXiEvent event = new LingXiEvent();
                event.setEventType(LingXiEventType.EXECUTION_INTERRUPTED);
                event.setStatus("FAILED");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("code", "EXECUTION_INTERRUPTED");
                payload.put("message", "后端服务重启，任务可重试");
                payload.put("retryable", true);
                event.setPayload(payload);
                String subtaskId = subtaskMapper.findByTaskId(task.getId()).stream()
                        .findFirst().map(subtask -> subtask.getId()).orElse(null);
                eventService.record(task.getId(), subtaskId, event, "lingxi:recovery:" + task.getId(),
                        null, "EXECUTION_INTERRUPTED", "后端服务重启，任务可重试");
                stopDifyBestEffort(task);
            } catch (Exception exception) {
                logger.error("Failed to recover interrupted AI task {}", task.getId(), exception);
            }
        }
    }

    private void stopDifyBestEffort(AiTask task) {
        if (StringUtils.isBlank(task.getDifyTaskId())) {
            return;
        }
        try {
            if ("WORKFLOW".equals(task.getTaskType())) {
                difyGateway.stopWorkflow(task.getDifyTaskId(), task.getUserId());
            } else {
                difyGateway.stopChatMessage(DifyChatApplication.CHATFLOW, task.getDifyTaskId(), task.getUserId());
            }
        } catch (Exception exception) {
            logger.warn("Best-effort Dify stop failed for interrupted AI task {}", task.getId());
        }
    }
}
