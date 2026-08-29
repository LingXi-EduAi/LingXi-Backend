package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiAuditLogMapper;
import com.lxe.lx.pojo.AiAuditLog;
import com.lxe.lx.service.AiAuditLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiAuditLogServiceImpl implements AiAuditLogService {

    private final AiAuditLogMapper auditLogMapper;

    public AiAuditLogServiceImpl(AiAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public AiAuditLog record(String userId, String path, String method, String ip) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(path)) {
            return null;
        }
        AiAuditLog log = new AiAuditLog();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(userId);
        log.setPath(path);
        log.setMethod(method);
        log.setIp(ip);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
        return log;
    }

    @Override
    public int deleteOlderThan(LocalDateTime cutoff) {
        if (cutoff == null) {
            return 0;
        }
        return auditLogMapper.deleteOlderThan(cutoff);
    }
}
