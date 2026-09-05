package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiModelCallLogMapper;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.StudentModelUsage;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void aggregateByStudentClassSkipsBlankClassId() {
        List<StudentModelUsage> result = service.aggregateByStudentClass("");
        assertThat(result).isEmpty();

        result = service.aggregateByStudentClass(null);
        assertThat(result).isEmpty();
    }

    @Test
    void aggregateByStudentClassDelegatesAndReturnsRows() {
        StudentModelUsage row = new StudentModelUsage();
        row.setUserId("stu-1");
        row.setCallCount(3L);
        row.setTotalTokens(1200L);
        row.setTotalLatencyMs(900L);
        row.setTotalCost(new BigDecimal("0.06"));
        row.setFailedCount(1L);

        when(mapper.aggregateByStudentClass("class-1")).thenReturn(Collections.singletonList(row));

        List<StudentModelUsage> result = service.aggregateByStudentClass("class-1");

        verify(mapper).aggregateByStudentClass("class-1");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("stu-1");
        assertThat(result.get(0).getTotalTokens()).isEqualTo(1200L);
        assertThat(result.get(0).getTotalCost()).isEqualByComparingTo("0.06");
    }
}
