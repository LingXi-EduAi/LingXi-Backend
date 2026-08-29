package com.lxe.lx.service;

import com.lxe.lx.pojo.AiAuditLog;

import java.time.LocalDateTime;

/**
 * AI 访问审计服务。
 */
public interface AiAuditLogService {

    /**
     * 记录一次 AI 访问审计。
     *
     * @param userId 操作用户 ID
     * @param path   请求路径
     * @param method HTTP 方法
     * @param ip     客户端 IP
     * @return 已保存的审计记录；参数为空时返回 {@code null}
     */
    AiAuditLog record(String userId, String path, String method, String ip);

    /**
     * 删除创建时间早于 cutoff 的审计记录（保留期清理）。
     *
     * @param cutoff 截止时间（早于该时间的记录被删除）
     * @return 删除行数
     */
    int deleteOlderThan(LocalDateTime cutoff);
}
