package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;

public interface AiTaskService {
    AiTaskResponse sendTask(AiTaskRequest request, String userId);
}
