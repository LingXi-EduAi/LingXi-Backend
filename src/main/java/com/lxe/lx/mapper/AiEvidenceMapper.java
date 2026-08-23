package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AiEvidenceMapper {
    int insert(AiEvidence evidence);

    List<AiEvidence> findByMessageId(@Param("messageId") String messageId);
}
