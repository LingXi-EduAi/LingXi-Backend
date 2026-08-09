package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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
}
