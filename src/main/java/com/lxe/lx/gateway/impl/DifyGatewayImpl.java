package com.lxe.lx.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DifyGatewayImpl implements DifyGateway {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Credentials legacyCredentials;
    private final Credentials chatflowCredentials;
    private final Credentials workflowCredentials;

    public DifyGatewayImpl(
            @Qualifier("difyRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${api.base-url}") String legacyBaseUrl,
            @Value("${api.key}") String legacyApiKey,
            @Value("${dify.chatflow.base-url}") String chatflowBaseUrl,
            @Value("${dify.chatflow.api-key}") String chatflowApiKey,
            @Value("${dify.workflow.base-url}") String workflowBaseUrl,
            @Value("${dify.workflow.api-key}") String workflowApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.legacyCredentials = new Credentials("旧版 Chatflow", legacyBaseUrl, legacyApiKey);
        this.chatflowCredentials = new Credentials("Chatflow", chatflowBaseUrl, chatflowApiKey);
        this.workflowCredentials = new Credentials("Workflow", workflowBaseUrl, workflowApiKey);
    }

    @Override
    public JsonNode sendChatMessage(
            DifyChatApplication application,
            DifyChatflowRequest request,
            String userId) {
        if (request == null) {
            throw invalid("Chatflow 请求不能为空");
        }
        requireUser(userId);
        Credentials credentials = chatCredentials(application);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", request.getInputs() == null ? Collections.emptyMap() : request.getInputs());
        body.put("query", request.getQuery());
        body.put("response_mode", "blocking");
        body.put("conversation_id", StringUtils.defaultString(request.getConversationId()));
        body.put("user", userId);
        body.put("files", request.getFiles() == null ? Collections.emptyList() : request.getFiles());
        body.put("auto_generate_name", request.isAutoGenerateName());

        return exchangeJson(
                credentials.name + " 消息调用",
                uri(credentials, "/chat-messages").build().encode().toUri(),
                HttpMethod.POST,
                jsonEntity(credentials, body)
        );
    }

    @Override
    public JsonNode getMessages(
            DifyChatApplication application,
            String conversationId,
            String userId,
            int limit,
            String firstId) {
        requireUser(userId);
        requireText(conversationId, "conversationId 不能为空");
        Credentials credentials = chatCredentials(application);
        UriComponentsBuilder uriBuilder = uri(credentials, "/messages")
                .queryParam("user", userId)
                .queryParam("conversation_id", conversationId)
                .queryParam("limit", limit);
        if (StringUtils.isNotBlank(firstId)) {
            uriBuilder.queryParam("first_id", firstId);
        }
        return exchangeJson(
                credentials.name + " 历史消息查询",
                uriBuilder.build().encode().toUri(),
                HttpMethod.GET,
                authorizedEntity(credentials)
        );
    }

    @Override
    public JsonNode getConversations(
            DifyChatApplication application,
            String userId,
            String lastId,
            int limit,
            String sortBy) {
        requireUser(userId);
        Credentials credentials = chatCredentials(application);
        UriComponentsBuilder uriBuilder = uri(credentials, "/conversations")
                .queryParam("user", userId)
                .queryParam("limit", limit)
                .queryParam("sort_by", sortBy);
        if (StringUtils.isNotBlank(lastId)) {
            uriBuilder.queryParam("last_id", lastId);
        }
        return exchangeJson(
                credentials.name + " 会话列表查询",
                uriBuilder.build().encode().toUri(),
                HttpMethod.GET,
                authorizedEntity(credentials)
        );
    }

    @Override
    public JsonNode deleteConversation(
            DifyChatApplication application,
            String conversationId,
            String userId) {
        requireUser(userId);
        requireText(conversationId, "conversationId 不能为空");
        Credentials credentials = chatCredentials(application);
        Map<String, Object> body = new HashMap<>();
        body.put("user", userId);
        return exchangeJson(
                credentials.name + " 会话删除",
                conversationUri(credentials, conversationId, null),
                HttpMethod.DELETE,
                jsonEntity(credentials, body)
        );
    }

    @Override
    public JsonNode renameConversation(
            DifyChatApplication application,
            String conversationId,
            String userId,
            String newName,
            boolean autoGenerate) {
        requireUser(userId);
        requireText(conversationId, "conversationId 不能为空");
        Credentials credentials = chatCredentials(application);
        Map<String, Object> body = new HashMap<>();
        if (!autoGenerate) {
            body.put("name", newName);
        }
        body.put("auto_generate", autoGenerate);
        body.put("user", userId);
        return exchangeJson(
                credentials.name + " 会话重命名",
                conversationUri(credentials, conversationId, "name"),
                HttpMethod.POST,
                jsonEntity(credentials, body)
        );
    }

    @Override
    public JsonNode uploadFile(DifyChatApplication application, MultipartFile file, String userId) {
        return upload(application, file, userId, "/files/upload", "文件上传");
    }

    @Override
    public JsonNode audioToText(DifyChatApplication application, MultipartFile file, String userId) {
        return upload(application, file, userId, "/audio-to-text", "语音转文字");
    }

    @Override
    public JsonNode runWorkflow(Map<String, Object> inputs, String userId) {
        requireUser(userId);
        workflowCredentials.validate();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs == null ? Collections.emptyMap() : inputs);
        body.put("response_mode", "blocking");
        body.put("user", userId);
        return exchangeJson(
                "Workflow 调用",
                uri(workflowCredentials, "/workflows/run").build().encode().toUri(),
                HttpMethod.POST,
                jsonEntity(workflowCredentials, body)
        );
    }

    private JsonNode upload(
            DifyChatApplication application,
            MultipartFile file,
            String userId,
            String path,
            String operation) {
        requireUser(userId);
        if (file == null || file.isEmpty()) {
            throw invalid("上传文件不能为空");
        }
        Credentials credentials = chatCredentials(application);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        } catch (IOException e) {
            throw new DifyGatewayException("读取上传文件失败", null, false, e);
        }
        body.add("user", userId);

        HttpHeaders headers = authorizedHeaders(credentials);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return exchangeJson(
                credentials.name + " " + operation,
                uri(credentials, path).build().encode().toUri(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers)
        );
    }

    private JsonNode exchangeJson(
            String operation,
            URI uri,
            HttpMethod method,
            HttpEntity<?> entity) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(uri, method, entity, JsonNode.class);
            if (response.getBody() == null) {
                throw new DifyGatewayException(operation + "返回空响应", null, false, null);
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            String detail = extractErrorDetail(e);
            throw new DifyGatewayException(
                    operation + "失败：" + detail,
                    e.getRawStatusCode(),
                    e.getRawStatusCode() == 429 || e.getRawStatusCode() >= 500,
                    e
            );
        } catch (ResourceAccessException e) {
            throw new DifyGatewayException("无法连接 Dify 服务：" + operation, null, true, e);
        }
    }

    private String extractErrorDetail(HttpStatusCodeException exception) {
        String detail = exception.getResponseBodyAsString();
        try {
            JsonNode body = objectMapper.readTree(detail);
            if (body.hasNonNull("message")) {
                detail = body.get("message").asText();
            }
        } catch (Exception ignored) {
            // Preserve a shortened response when Dify does not return JSON.
        }
        if (StringUtils.isBlank(detail)) {
            detail = exception.getStatusText();
        }
        return StringUtils.abbreviate(detail, 500);
    }

    private Credentials chatCredentials(DifyChatApplication application) {
        if (application == null) {
            throw invalid("Dify Chatflow 应用不能为空");
        }
        Credentials credentials = application == DifyChatApplication.LEGACY
                ? legacyCredentials
                : chatflowCredentials;
        credentials.validate();
        return credentials;
    }

    private UriComponentsBuilder uri(Credentials credentials, String path) {
        credentials.validate();
        return UriComponentsBuilder.fromHttpUrl(credentials.baseUrl).path(path);
    }

    private URI conversationUri(Credentials credentials, String conversationId, String action) {
        UriComponentsBuilder builder = uri(credentials, "/conversations/").pathSegment(conversationId);
        if (StringUtils.isNotBlank(action)) {
            builder.pathSegment(action);
        }
        return builder.build().encode().toUri();
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Credentials credentials, Map<String, Object> body) {
        HttpHeaders headers = authorizedHeaders(credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authorizedEntity(Credentials credentials) {
        return new HttpEntity<>(authorizedHeaders(credentials));
    }

    private HttpHeaders authorizedHeaders(Credentials credentials) {
        credentials.validate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.apiKey);
        return headers;
    }

    private void requireUser(String userId) {
        requireText(userId, "Dify user 不能为空");
    }

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw invalid(message);
        }
    }

    private DifyGatewayException invalid(String message) {
        return new DifyGatewayException(message, null, false, null);
    }

    private static class Credentials {
        private final String name;
        private final String baseUrl;
        private final String apiKey;

        private Credentials(String name, String baseUrl, String apiKey) {
            this.name = name;
            this.baseUrl = StringUtils.removeEnd(StringUtils.defaultString(baseUrl), "/");
            this.apiKey = apiKey;
        }

        private void validate() {
            if (StringUtils.isBlank(baseUrl)) {
                throw new DifyGatewayException(name + " BASE_URL 未配置", null, false, null);
            }
            if (StringUtils.isBlank(apiKey)) {
                throw new DifyGatewayException(name + " API_KEY 未配置", null, false, null);
            }
        }
    }
}
