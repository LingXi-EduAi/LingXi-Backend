package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface AiEvidenceMapper {
    int insert(AiEvidence evidence);

    List<AiEvidence> findByMessageId(@Param("messageId") String messageId);

    /** 删除创建时间早于 cutoff 的证据，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** 删除指定会话下所有消息关联的证据，返回删除行数。 */
    int deleteByConversation(@Param("conversationId") String conversationId);
}
