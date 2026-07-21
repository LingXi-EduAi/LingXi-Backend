package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.service.ApiService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service("ApiService")
public class ApiServiceImpl implements ApiService {
    private final DifyGateway difyGateway;

    public ApiServiceImpl(DifyGateway difyGateway) {
        this.difyGateway = difyGateway;
    }

    @Override
    public String sendMessage(String userInput, String userId, String conversationId) {
        DifyChatflowRequest request = new DifyChatflowRequest();
        request.setQuery(userInput);
        request.setConversationId(conversationId);
        return json(difyGateway.sendChatMessage(DifyChatApplication.LEGACY, request, userId));
    }

    @Override
    public String fileUpload(MultipartFile file, String userId) {
        return json(difyGateway.uploadFile(DifyChatApplication.LEGACY, file, userId));
    }

    @Override
    public String renameConversation(
            String conversationId,
            String userId,
            String newName,
            boolean autoGenerate) {
        return json(difyGateway.renameConversation(
                DifyChatApplication.LEGACY,
                conversationId,
                userId,
                newName,
                autoGenerate
        ));
    }

    @Override
    public String audioToText(MultipartFile file, String userId) {
        return json(difyGateway.audioToText(DifyChatApplication.LEGACY, file, userId));
    }

    @Override
    public String getMessages(String conversationId, String userId, int limit, String firstId) {
        return json(difyGateway.getMessages(
                DifyChatApplication.LEGACY,
                conversationId,
                userId,
                limit,
                firstId
        ));
    }

    @Override
    public String getConversations(String userId, String lastId, int limit, String sortBy) {
        return json(difyGateway.getConversations(
                DifyChatApplication.LEGACY,
                userId,
                lastId,
                limit,
                sortBy
        ));
    }

    @Override
    public String deleteConversation(String conversationId, String userId) {
        return json(difyGateway.deleteConversation(
                DifyChatApplication.LEGACY,
                conversationId,
                userId
        ));
    }

    private String json(JsonNode response) {
        return response.toString();
    }
}
