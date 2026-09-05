package com.lxe.lx.service;

import com.lxe.lx.domain.qo.AiModelCallLogQuery;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.StudentModelUsage;

import java.util.List;

public interface AiModelCallLogService {
    void recordAsync(AiModelCallLog log);

    List<AiModelCallLog> findByTaskId(String taskId, String userId);

    List<AiModelCallLog> findByQuery(AiModelCallLogQuery query);

    List<AiModelCallLog> findAllByQuery(AiModelCallLogQuery query);

    int countByQuery(AiModelCallLogQuery query);

    List<StudentModelUsage> aggregateByStudentClass(String classId);
}
