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

    /** 删除创建时间早于 cutoff 的事件，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** 删除指定任务下的所有事件，返回删除行数。 */
    int deleteByTask(@Param("taskId") String taskId);
}
