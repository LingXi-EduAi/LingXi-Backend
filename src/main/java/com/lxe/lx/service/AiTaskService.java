package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import com.lxe.lx.domain.dto.AiTaskCreateRequest;
import com.lxe.lx.domain.dto.AiTaskCreateResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiTaskService {
    AiTaskResponse sendTask(AiTaskRequest request, String userId);

    SseEmitter streamTask(AiTaskRequest request, String userId);

    AiTaskCreateResponse createTask(AiTaskCreateRequest request, String userId);
}
