package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiFeedback {
    private String id;
    private String taskId;
    private String conversationId;
    private String userId;
    private String summary;
    /** JSON 字符串数组：薄弱知识点。 */
    private String weakPointsJson;
    /** JSON 字符串数组：推荐练习。 */
    private String recommendationsJson;
    private String nextStep;
    /** VALIDATION_FINISHED 或 RULE_FALLBACK。 */
    private String source;
    /** 来源 validation_finished 事件的原始 payload，用于追溯。 */
    private String rawPayloadJson;
    private LocalDateTime createdAt;
}
