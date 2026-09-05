package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.AiModelCallLogQuery;
import com.lxe.lx.mapper.AiModelCallLogMapper;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.StudentModelUsage;
import com.lxe.lx.service.AiModelCallLogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AiModelCallLogServiceImpl implements AiModelCallLogService {
    private static final Logger logger = LogManager.getLogger(AiModelCallLogServiceImpl.class);
    private final AiModelCallLogMapper mapper;
    private final TaskExecutor executor;

    public AiModelCallLogServiceImpl(
            AiModelCallLogMapper mapper,
            @Qualifier("aiModelCallLogExecutor") TaskExecutor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }

    @Override
    public void recordAsync(AiModelCallLog log) {
        if (log == null) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    mapper.insert(log);
                } catch (RuntimeException exception) {
                    logger.warn("AI model call log write failed, taskId={}", log.getTaskId(), exception);
                }
            });
        } catch (RuntimeException exception) {
            logger.warn("AI model call log task rejected, taskId={}", log.getTaskId(), exception);
        }
    }

    @Override
    public List<AiModelCallLog> findByTaskId(String taskId, String userId) {
        if (taskId == null || userId == null) {
            return Collections.emptyList();
        }
        return mapper.findByTaskId(taskId, userId);
    }

    @Override
    public List<AiModelCallLog> findByQuery(AiModelCallLogQuery query) {
        return mapper.findByQuery(query);
    }

    @Override
    public List<AiModelCallLog> findAllByQuery(AiModelCallLogQuery query) {
        return mapper.findAllByQuery(query);
    }

    @Override
    public int countByQuery(AiModelCallLogQuery query) {
        return mapper.countByQuery(query);
    }

    @Override
    public List<StudentModelUsage> aggregateByStudentClass(String classId) {
        if (classId == null || classId.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.aggregateByStudentClass(classId);
    }
}
