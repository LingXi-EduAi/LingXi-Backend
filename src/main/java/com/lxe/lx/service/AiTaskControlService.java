package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiTaskSnapshot;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiTaskControlService {
    AiTaskSnapshot getSnapshot(String taskId, String userId);

    SseEmitter subscribe(String taskId, String userId, String lastEventId);

    AiTaskSnapshot stop(String taskId, String userId);

    AiTaskSnapshot retry(String taskId, String subtaskId, String userId);
}
