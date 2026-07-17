package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.service.DifyChatflowService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class DifyChatflowServiceImpl implements DifyChatflowService {
    private final RestTemplate restTemplate;
    private final String chatMessagesUrl;
    private final String messagesUrl;
    private final String apiKey;

    public DifyChatflowServiceImpl(
            @Qualifier("difyRestTemplate") RestTemplate restTemplate,
            @Value("${dify.chatflow.base-url}") String baseUrl,
            @Value("${dify.chatflow.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        String normalizedBaseUrl = StringUtils.removeEnd(baseUrl, "/");
        this.chatMessagesUrl = normalizedBaseUrl + "/chat-messages";
        this.messagesUrl = normalizedBaseUrl + "/messages";
        this.apiKey = apiKey;
    }

    @Override
    public JsonNode sendMessage(DifyChatflowRequest request, String userId) {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("DIFY_CHATFLOW_API_KEY 未配置");
        }
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("Dify user 不能为空");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", request.getInputs());
        requestBody.put("query", request.getQuery());
        requestBody.put("response_mode", "blocking");
        requestBody.put("conversation_id", request.getConversationId());
        requestBody.put("user", userId);
        requestBody.put("files", request.getFiles());
        requestBody.put("auto_generate_name", request.isAutoGenerateName());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                chatMessagesUrl,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("Dify Chatflow 返回空响应");
        }
        return response.getBody();
    }

    @Override
    public JsonNode getMessages(String conversationId, String userId, int limit, String firstId) {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("DIFY_CHATFLOW_API_KEY 未配置");
        }
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("Dify user 不能为空");
        }
        if (StringUtils.isBlank(conversationId)) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(messagesUrl)
                .queryParam("user", userId)
                .queryParam("conversation_id", conversationId)
                .queryParam("limit", limit);
        if (StringUtils.isNotBlank(firstId)) {
            uriBuilder.queryParam("first_id", firstId);
        }
        URI uri = uriBuilder.build().encode().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("Dify Chatflow 历史消息返回空响应");
        }
        return response.getBody();
    }
}
