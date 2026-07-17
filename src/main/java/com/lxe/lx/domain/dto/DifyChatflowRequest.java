package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DifyChatflowRequest {
    private String query;
    private Map<String, Object> inputs = new HashMap<>();
    private String conversationId = "";
    private List<Map<String, Object>> files = new ArrayList<>();
    private boolean autoGenerateName = true;
}
