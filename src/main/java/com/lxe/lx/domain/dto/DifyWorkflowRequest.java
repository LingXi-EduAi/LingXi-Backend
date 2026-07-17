package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DifyWorkflowRequest {
    private Map<String, Object> inputs = new HashMap<>();
}
