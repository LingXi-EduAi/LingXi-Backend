package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.AiModelCallLogQuery;
import com.lxe.lx.pojo.AiModelCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface AiModelCallLogMapper {
    int insert(AiModelCallLog log);

    List<AiModelCallLog> findByTaskId(
            @Param("taskId") String taskId,
            @Param("userId") String userId
    );

    List<AiModelCallLog> findByQuery(AiModelCallLogQuery query);

    List<AiModelCallLog> findAllByQuery(AiModelCallLogQuery query);

    int countByQuery(AiModelCallLogQuery query);

    /** 删除创建时间早于 cutoff 的模型调用日志，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
