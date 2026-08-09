package com.lxe.lx.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface DifyGateway {
    JsonNode sendChatMessage(DifyChatApplication application, DifyChatflowRequest request, String userId);

    DifyStream streamChatMessage(
            DifyChatApplication application,
            DifyChatflowRequest request,
            String userId,
            DifyStreamListener listener
    );

    DifyStream streamWorkflow(
            Map<String, Object> inputs,
            String userId,
            DifyStreamListener listener
    );

    void stopChatMessage(DifyChatApplication application, String difyTaskId, String userId);

    void stopWorkflow(String difyTaskId, String userId);

    JsonNode getMessages(
            DifyChatApplication application,
            String conversationId,
            String userId,
            int limit,
            String firstId
    );

    JsonNode getConversations(
            DifyChatApplication application,
            String userId,
            String lastId,
            int limit,
            String sortBy
    );

    JsonNode deleteConversation(DifyChatApplication application, String conversationId, String userId);

    JsonNode renameConversation(
            DifyChatApplication application,
            String conversationId,
            String userId,
            String newName,
            boolean autoGenerate
    );

    JsonNode uploadFile(DifyChatApplication application, MultipartFile file, String userId);

    JsonNode audioToText(DifyChatApplication application, MultipartFile file, String userId);

    JsonNode runWorkflow(Map<String, Object> inputs, String userId);
}
