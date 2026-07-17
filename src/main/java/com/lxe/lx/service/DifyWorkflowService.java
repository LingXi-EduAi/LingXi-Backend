package com.lxe.lx.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface DifyWorkflowService {
    JsonNode runWorkflow(Map<String, Object> inputs, String userId);
}
