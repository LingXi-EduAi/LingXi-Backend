package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskRequest {
    private String query;
    private String conversationId = "";
}
