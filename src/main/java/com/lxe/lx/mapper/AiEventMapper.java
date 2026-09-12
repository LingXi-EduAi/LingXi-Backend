package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface AiEventMapper {
    int insert(AiEvent event);

    AiEvent findBySourceEventId(
            @Param("taskId") String taskId,
            @Param("sourceEventId") String sourceEventId
    );

    List<AiEvent> findAfterSequence(
            @Param("taskId") String taskId,
            @Param("sequence") long sequence
    );

    /** 查询任务下指定类型的最新一条事件，用于反思等后置处理。 */
    AiEvent findLatestByTaskAndType(
            @Param("taskId") String taskId,
            @Param("eventType") String eventType
    );

    /** 删除创建时间早于 cutoff 的事件，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** 删除指定任务下的所有事件，返回删除行数。 */
    int deleteByTask(@Param("taskId") String taskId);
}
