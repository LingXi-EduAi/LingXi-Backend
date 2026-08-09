package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AiTaskMapper {
    int insert(AiTask task);

    AiTask findById(@Param("id") String id);

    AiTask findByIdAndUser(@Param("id") String id, @Param("userId") String userId);

    AiTask lockById(@Param("id") String id);

    AiTask findLatestByConversation(
            @Param("userId") String userId,
            @Param("conversationId") String conversationId
    );

    AiTask findLatestByConversationId(@Param("conversationId") String conversationId);

    int updateAfterEvent(AiTask task);

    int updateDifyIds(
            @Param("id") String id,
            @Param("difyTaskId") String difyTaskId,
            @Param("difyConversationId") String difyConversationId
    );

    List<AiTask> findRunningTasks();

    int restartForRetry(AiTask task);
}
