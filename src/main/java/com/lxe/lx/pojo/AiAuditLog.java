package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 访问审计日志实体。
 */
@Getter
@Setter
public class AiAuditLog {
    private String id;
    private String userId;
    private String path;
    private String method;
    private String ip;
    private LocalDateTime createdAt;
}
