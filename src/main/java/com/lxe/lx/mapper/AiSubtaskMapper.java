package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiSubtask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AiSubtaskMapper {
    int insert(AiSubtask subtask);

    AiSubtask findByIdAndTask(@Param("id") String id, @Param("taskId") String taskId);

    AiSubtask lockByIdAndTask(@Param("id") String id, @Param("taskId") String taskId);

    List<AiSubtask> findByTaskId(@Param("taskId") String taskId);

    int updateAssignment(AiSubtask subtask);

    int updateStatus(AiSubtask subtask);

    int prepareRetry(AiSubtask subtask);
}
