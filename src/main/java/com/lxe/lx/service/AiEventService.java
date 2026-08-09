package com.lxe.lx.service;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.pojo.AiEvent;

public interface AiEventService {
    AiEvent record(
            String taskId,
            String subtaskId,
            LingXiEvent event,
            String sourceEventId,
            String resultJson,
            String errorCode,
            String errorMessage
    );

    LingXiEvent toContract(AiEvent event, String conversationId);
}
