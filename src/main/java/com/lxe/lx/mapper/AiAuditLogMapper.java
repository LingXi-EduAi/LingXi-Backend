package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Mapper
@Repository
public interface AiAuditLogMapper {
    int insert(AiAuditLog log);

    /** 删除创建时间早于 cutoff 的审计记录，返回删除行数。 */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
