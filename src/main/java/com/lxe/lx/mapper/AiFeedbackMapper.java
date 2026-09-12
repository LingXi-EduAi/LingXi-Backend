package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Mapper
@Repository
public interface AiFeedbackMapper {
    int insert(AiFeedback feedback);

    AiFeedback findByTaskId(@Param("taskId") String taskId);

    /** 删除创建时间早于 cutoff 的反馈，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** 删除指定任务下的反馈，返回删除行数。 */
    int deleteByTask(@Param("taskId") String taskId);

    /** 删除指定会话下的反馈，返回删除行数。 */
    int deleteByConversation(@Param("conversationId") String conversationId);
}
