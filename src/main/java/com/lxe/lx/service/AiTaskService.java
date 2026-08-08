package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiTaskService {
    AiTaskResponse sendTask(AiTaskRequest request, String userId);

    SseEmitter streamTask(AiTaskRequest request, String userId);
}
