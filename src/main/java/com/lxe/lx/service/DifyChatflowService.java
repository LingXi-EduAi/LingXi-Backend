package com.lxe.lx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;

public interface DifyChatflowService {
    JsonNode sendMessage(DifyChatflowRequest request, String userId);

    JsonNode getMessages(String conversationId, String userId, int limit, String firstId);
}
