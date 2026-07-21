package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.service.DifyChatflowService;
import org.springframework.stereotype.Service;

@Service
public class DifyChatflowServiceImpl implements DifyChatflowService {
    private final DifyGateway difyGateway;

    public DifyChatflowServiceImpl(DifyGateway difyGateway) {
        this.difyGateway = difyGateway;
    }

    @Override
    public JsonNode sendMessage(DifyChatflowRequest request, String userId) {
        return difyGateway.sendChatMessage(DifyChatApplication.CHATFLOW, request, userId);
    }

    @Override
    public JsonNode getMessages(String conversationId, String userId, int limit, String firstId) {
        return difyGateway.getMessages(
                DifyChatApplication.CHATFLOW,
                conversationId,
                userId,
                limit,
                firstId
        );
    }
}
