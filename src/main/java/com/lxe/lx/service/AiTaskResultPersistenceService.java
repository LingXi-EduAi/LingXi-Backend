package com.lxe.lx.service;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.pojo.AiEvidence;

import java.util.List;

public interface AiTaskResultPersistenceService {
    void recordTerminalEvent(
            String taskId,
            String subtaskId,
            LingXiEvent event,
            String sourceEventId,
            String resultJson,
            String errorCode,
            String errorMessage,
            String answer,
            String difyMessageId,
            List<AiEvidence> evidences
    );
}
