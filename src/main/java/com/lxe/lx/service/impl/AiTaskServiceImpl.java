package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.AiAgentRouter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.service.AiTaskService;
import org.springframework.stereotype.Service;

@Service
public class AiTaskServiceImpl implements AiTaskService {

    private final DifyGateway difyGateway;
    private final AiAgentRouter agentRouter;

    public AiTaskServiceImpl(DifyGateway difyGateway, AiAgentRouter agentRouter) {
        this.difyGateway = difyGateway;
        this.agentRouter = agentRouter;
    }

    @Override
    public AiTaskResponse sendTask(AiTaskRequest request, String userId) {
        long startMs = System.currentTimeMillis();

        DifyChatflowRequest difyRequest = new DifyChatflowRequest();
        difyRequest.setQuery(request.getQuery());
        difyRequest.setConversationId(request.getConversationId());

        JsonNode result = difyGateway.sendChatMessage(
                agentRouter.route(request.getQuery()),
                difyRequest,
                userId
        );

        long elapsedMs = System.currentTimeMillis() - startMs;

        AiTaskResponse response = new AiTaskResponse();
        response.setAnswer(result.has("answer") ? result.get("answer").asText() : "");
        response.setConversationId(result.has("conversation_id") ? result.get("conversation_id").asText() : "");
        response.setMessageId(result.has("message_id") ? result.get("message_id").asText() : "");
        response.setElapsedMs(elapsedMs);
        return response;
    }
}
