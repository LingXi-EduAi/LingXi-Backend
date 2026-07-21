package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.service.DifyWorkflowService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DifyWorkflowServiceImpl implements DifyWorkflowService {
    private final DifyGateway difyGateway;

    public DifyWorkflowServiceImpl(DifyGateway difyGateway) {
        this.difyGateway = difyGateway;
    }

    @Override
    public JsonNode runWorkflow(Map<String, Object> inputs, String userId) {
        return difyGateway.runWorkflow(inputs, userId);
    }
}
