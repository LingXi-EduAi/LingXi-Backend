package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiMessage {
    private String id;
    private String conversationId;
    private String taskId;
    private String role;
    private String content;
    private String status;
    private String difyMessageId;
    /** 附件 JSON（text 存储，FastJSON 风格），无附件时为 null。 */
    private String attachments;
    /** 失败时的错误码，成功消息为 null。 */
    private String errorCode;
    /** 失败时的错误信息，成功消息为 null。 */
    private String errorMessage;
    private LocalDateTime createdAt;
}
