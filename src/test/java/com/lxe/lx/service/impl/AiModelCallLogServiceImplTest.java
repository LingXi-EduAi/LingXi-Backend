package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiModelCallLogMapper;
import com.lxe.lx.pojo.AiModelCallLog;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiModelCallLogServiceImplTest {
    private final AiModelCallLogMapper mapper = mock(AiModelCallLogMapper.class);
    private final TaskExecutor directExecutor = Runnable::run;
    private final AiModelCallLogServiceImpl service =
            new AiModelCallLogServiceImpl(mapper, directExecutor);

    @Test
    void writesAsynchronouslyThroughExecutor() {
        AiModelCallLog log = new AiModelCallLog();

        service.recordAsync(log);

        verify(mapper).insert(log);
    }

    @Test
    void mapperFailureDoesNotEscapeToTaskExecution() {
        doThrow(new RuntimeException("database unavailable"))
                .when(mapper).insert(any(AiModelCallLog.class));

        service.recordAsync(new AiModelCallLog());
    }
}
