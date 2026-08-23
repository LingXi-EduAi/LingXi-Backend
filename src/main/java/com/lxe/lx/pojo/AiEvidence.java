package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AiEvidence {
    private String id;
    private String messageId;
    private String sourceType;
    private String title;
    private String url;
    private String contentSnippet;
    private BigDecimal score;
    private LocalDateTime createdAt;
}
