package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface AiMessageMapper {
    int insert(AiMessage message);

    AiMessage findByDifyMessageId(@Param("difyMessageId") String difyMessageId);

    AiMessage findByTaskAndRole(
            @Param("taskId") String taskId,
            @Param("role") String role
    );

    List<AiMessage> findByConversation(
            @Param("conversationId") String conversationId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByConversation(@Param("conversationId") String conversationId);

    /** 删除创建时间早于 cutoff 的消息，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** 删除指定会话下的所有消息，返回删除行数。 */
    int deleteByConversation(@Param("conversationId") String conversationId);
}
