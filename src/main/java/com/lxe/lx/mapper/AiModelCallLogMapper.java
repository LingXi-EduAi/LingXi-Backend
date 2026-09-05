package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.AiModelCallLogQuery;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.StudentModelUsage;
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

    /**
     * 按班级聚合班内每个学生的 AI 用量（BE-08）。
     * 关联 ai_task（取 user_id）与 lx_customer（按 class_id 过滤学生），按学生分组。
     */
    List<StudentModelUsage> aggregateByStudentClass(@Param("classId") String classId);

    /** 删除创建时间早于 cutoff 的模型调用日志，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
