package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AiModelCallLog {
    private String id;
    private String taskId;
    private String userId;
    private String nodeName;
    private String model;
    private Long totalTokens;
    private Long latencyMs;
    private BigDecimal cost;
    private String errorCode;
    private LocalDateTime createdAt;
}
