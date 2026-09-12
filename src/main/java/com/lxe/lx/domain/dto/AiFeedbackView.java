package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 个性化反馈视图（BE-15）。
 */
@Getter
@Setter
public class AiFeedbackView {
    private String taskId;
    private String conversationId;
    private String summary;
    private List<String> weakPoints;
    private List<String> recommendations;
    private String nextStep;
    /** VALIDATION_FINISHED 或 RULE_FALLBACK。 */
    private String source;
    private LocalDateTime createdAt;
}
