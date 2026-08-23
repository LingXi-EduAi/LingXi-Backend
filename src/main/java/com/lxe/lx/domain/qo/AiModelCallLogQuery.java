package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiModelCallLogQuery {
    private String userId;
    private String taskId;
    private String nodeName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int offset;
    private int limit;
}
