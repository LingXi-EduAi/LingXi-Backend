package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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
}
