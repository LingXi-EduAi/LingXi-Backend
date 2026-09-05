package com.lxe.lx.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStreamListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DifyGatewayImpl 单元测试（纯 JUnit 5 + Mockito，无 Spring 上下文）。
 *
 * <p>阻塞方法（sendChatMessage / getConversations / getMessages / deleteConversation /
 * renameConversation / runWorkflow / stopChatMessage / stopWorkflow）全部走
 * {@link RestTemplate#exchange}，通过 mock RestTemplate 断言请求 URL、Header、Body 与响应解析。</p>
 *
 * <p>流式方法（streamChatMessage / streamWorkflow）在参数校验通过后会通过 OkHttpClient
 * 发起真实网络连接（{@code streamingClient.newCall(...).enqueue(...)}），无法在单元测试中
 * 无网络执行，因此仅覆盖参数校验路径（null 请求 / null 监听器 / 空 userId / 未配置凭据），
 * 真实 SSE 流式行为由集成测试对活 Dify 服务验证。</p>
 */
class DifyGatewayImplTest {

    private static final String LEGACY_BASE = "http://legacy.example.com";
    private static final String LEGACY_KEY = "legacy-key";
    private static final String CHATFLOW_BASE = "http://chatflow.example.com";
    private static final String CHATFLOW_KEY = "chatflow-key";
    private static final String WORKFLOW_BASE = "http://workflow.example.com";
    private static final String WORKFLOW_KEY = "workflow-key";

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private DifyGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        gateway = newGateway(LEGACY_BASE, LEGACY_KEY, CHATFLOW_BASE, CHATFLOW_KEY, WORKFLOW_BASE, WORKFLOW_KEY);
    }

    private DifyGatewayImpl newGateway(String legacyBase, String legacyKey,
                                       String chatflowBase, String chatflowKey,
                                       String workflowBase, String workflowKey) {
        return new DifyGatewayImpl(restTemplate, objectMapper,
                legacyBase, legacyKey, chatflowBase, chatflowKey,
                workflowBase, workflowKey, 5000L, 0L);
    }

    private void stubExchange(JsonNode body) {
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private DifyChatflowRequest chatflowRequest() {
        DifyChatflowRequest request = new DifyChatflowRequest();
        request.setQuery("1+1等于多少？");
        request.setConversationId("conversation-1");
        return request;
    }

    private DifyStreamListener listener() {
        return mock(DifyStreamListener.class);
    }

    // ===== sendChatMessage (blocking) =====

    @Test
    void sendChatMessagePostsToChatflowEndpointWithAuthAndBody() throws Exception {
        stubExchange(json("{\"answer\":\"2\"}"));
        DifyChatflowRequest request = chatflowRequest();

        JsonNode result = gateway.sendChatMessage(DifyChatApplication.CHATFLOW, request, "user-1");

        assertEquals("2", result.get("answer").asText());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/chat-messages", uriCaptor.getValue().toString());
        assertEquals("Bearer " + CHATFLOW_KEY, entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(MediaType.APPLICATION_JSON, entityCaptor.getValue().getHeaders().getContentType());
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("1+1等于多少？", body.get("query"));
        assertEquals("blocking", body.get("response_mode"));
        assertEquals("user-1", body.get("user"));
        assertEquals("conversation-1", body.get("conversation_id"));
        assertEquals(true, body.get("auto_generate_name"));
    }

    @Test
    void sendChatMessageUsesLegacyCredentialsForLegacyApp() throws Exception {
        stubExchange(json("{\"answer\":\"2\"}"));

        gateway.sendChatMessage(DifyChatApplication.LEGACY, chatflowRequest(), "user-1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(LEGACY_BASE + "/chat-messages", uriCaptor.getValue().toString());
        assertEquals("Bearer " + LEGACY_KEY, entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void sendChatMessageRejectsNullRequest() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(DifyChatApplication.CHATFLOW, null, "user-1"));
        assertEquals("Chatflow 请求不能为空", exception.getMessage());
    }

    @Test
    void sendChatMessageRejectsBlankUserId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), " "));
        assertEquals("Dify user 不能为空", exception.getMessage());
    }

    @Test
    void sendChatMessageRejectsNullApplication() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(null, chatflowRequest(), "user-1"));
        assertEquals("Dify Chatflow 应用不能为空", exception.getMessage());
    }

    // ===== getConversations / getMessages =====

    @Test
    void getConversationsBuildsQueryWithUserLimitSortBy() throws Exception {
        stubExchange(json("{\"data\":[]}"));

        gateway.getConversations(DifyChatApplication.CHATFLOW, "user-1", null, 20, "-updated_at");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/conversations?user=user-1&limit=20&sort_by=-updated_at",
                uriCaptor.getValue().toString());
    }

    @Test
    void getConversationsAppendsLastIdWhenProvided() throws Exception {
        stubExchange(json("{\"data\":[]}"));

        gateway.getConversations(DifyChatApplication.CHATFLOW, "user-1", "last-1", 20, "-updated_at");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/conversations?user=user-1&limit=20&sort_by=-updated_at&last_id=last-1",
                uriCaptor.getValue().toString());
    }

    @Test
    void getMessagesBuildsQueryWithConversationAndLimit() throws Exception {
        stubExchange(json("{\"data\":[]}"));

        gateway.getMessages(DifyChatApplication.CHATFLOW, "conversation-1", "user-1", 20, null);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/messages?user=user-1&conversation_id=conversation-1&limit=20",
                uriCaptor.getValue().toString());
    }

    @Test
    void getMessagesAppendsFirstIdWhenProvided() throws Exception {
        stubExchange(json("{\"data\":[]}"));

        gateway.getMessages(DifyChatApplication.CHATFLOW, "conversation-1", "user-1", 20, "first-1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/messages?user=user-1&conversation_id=conversation-1&limit=20&first_id=first-1",
                uriCaptor.getValue().toString());
    }

    @Test
    void getMessagesRejectsBlankConversationId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.getMessages(DifyChatApplication.CHATFLOW, "", "user-1", 20, null));
        assertEquals("conversationId 不能为空", exception.getMessage());
    }

    // ===== deleteConversation / renameConversation =====

    @Test
    void deleteConversationSendsDeleteWithUserBody() throws Exception {
        stubExchange(json("{\"result\":\"success\"}"));

        gateway.deleteConversation(DifyChatApplication.CHATFLOW, "conversation-1", "user-1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.DELETE), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/conversations/conversation-1", uriCaptor.getValue().toString());
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("user-1", body.get("user"));
    }

    @Test
    void renameConversationPostsNameWhenNotAutoGenerate() throws Exception {
        stubExchange(json("{\"result\":\"success\"}"));

        gateway.renameConversation(DifyChatApplication.CHATFLOW, "conversation-1", "user-1", "新名字", false);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/conversations/conversation-1/name", uriCaptor.getValue().toString());
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("新名字", body.get("name"));
        assertEquals(false, body.get("auto_generate"));
        assertEquals("user-1", body.get("user"));
    }

    @Test
    void renameConversationOmitsNameWhenAutoGenerate() throws Exception {
        stubExchange(json("{\"result\":\"success\"}"));

        gateway.renameConversation(DifyChatApplication.CHATFLOW, "conversation-1", "user-1", "ignored", true);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertFalse(body.containsKey("name"));
        assertEquals(true, body.get("auto_generate"));
    }

    // ===== runWorkflow (blocking) =====

    @Test
    void runWorkflowPostsToWorkflowEndpointWithAuthAndBody() throws Exception {
        stubExchange(json("{\"workflow_run_id\":\"run-1\"}"));
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("topic", "牛顿第二定律");

        JsonNode result = gateway.runWorkflow(inputs, "user-1");

        assertEquals("run-1", result.get("workflow_run_id").asText());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(WORKFLOW_BASE + "/workflows/run", uriCaptor.getValue().toString());
        assertEquals("Bearer " + WORKFLOW_KEY, entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(MediaType.APPLICATION_JSON, entityCaptor.getValue().getHeaders().getContentType());
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("blocking", body.get("response_mode"));
        assertEquals("user-1", body.get("user"));
        assertEquals("牛顿第二定律", ((Map<String, Object>) body.get("inputs")).get("topic"));
    }

    @Test
    void runWorkflowDefaultsNullInputsToEmptyMap() throws Exception {
        stubExchange(json("{\"workflow_run_id\":\"run-1\"}"));

        gateway.runWorkflow(null, "user-1");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals(Collections.emptyMap(), body.get("inputs"));
    }

    @Test
    void runWorkflowRejectsBlankUserId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.runWorkflow(Collections.emptyMap(), ""));
        assertEquals("Dify user 不能为空", exception.getMessage());
    }

    @Test
    void runWorkflowRejectsUnconfiguredWorkflowCredentials() {
        DifyGatewayImpl unconfigured = newGateway(LEGACY_BASE, LEGACY_KEY, CHATFLOW_BASE, CHATFLOW_KEY, "", WORKFLOW_KEY);

        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> unconfigured.runWorkflow(Collections.emptyMap(), "user-1"));

        assertEquals("Workflow BASE_URL 未配置", exception.getMessage());
    }

    // ===== stopChatMessage / stopWorkflow（阻塞调用，走 mock 的 RestTemplate） =====

    @Test
    void stopChatMessagePostsStopForTask() throws Exception {
        stubExchange(json("{\"result\":\"success\"}"));

        gateway.stopChatMessage(DifyChatApplication.CHATFLOW, "dify-task-1", "user-1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(JsonNode.class));
        assertEquals(CHATFLOW_BASE + "/chat-messages/dify-task-1/stop", uriCaptor.getValue().toString());
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("user-1", body.get("user"));
    }

    @Test
    void stopWorkflowPostsStopForTask() throws Exception {
        stubExchange(json("{\"result\":\"success\"}"));

        gateway.stopWorkflow("dify-task-1", "user-1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), any(), eq(JsonNode.class));
        assertEquals(WORKFLOW_BASE + "/workflows/tasks/dify-task-1/stop", uriCaptor.getValue().toString());
    }

    @Test
    void stopChatMessageRejectsBlankTaskId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.stopChatMessage(DifyChatApplication.CHATFLOW, "", "user-1"));
        assertEquals("Dify taskId 不能为空", exception.getMessage());
    }

    @Test
    void stopWorkflowRejectsBlankUserId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.stopWorkflow("dify-task-1", null));
        assertEquals("Dify user 不能为空", exception.getMessage());
    }

    // ===== 流式方法：仅参数校验路径（无网络） =====
    // 说明：streamChatMessage / streamWorkflow 在参数校验通过后会通过
    // streamingClient.newCall(...).enqueue(...) 发起真实 OkHttp 网络连接，
    // 单元测试无法无网络执行，因此只覆盖校验路径；真实 SSE 流式行为由
    // 集成测试对活 Dify 服务验证。

    @Test
    void streamChatMessageRejectsNullRequest() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.streamChatMessage(DifyChatApplication.CHATFLOW, null, "user-1", listener()));
        assertEquals("Chatflow 请求不能为空", exception.getMessage());
    }

    @Test
    void streamChatMessageRejectsNullListener() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.streamChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), "user-1", null));
        assertEquals("流式事件监听器不能为空", exception.getMessage());
    }

    @Test
    void streamChatMessageRejectsBlankUserId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.streamChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), " ", listener()));
        assertEquals("Dify user 不能为空", exception.getMessage());
    }

    @Test
    void streamWorkflowRejectsBlankUserId() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.streamWorkflow(Collections.emptyMap(), "", listener()));
        assertEquals("Dify user 不能为空", exception.getMessage());
    }

    @Test
    void streamWorkflowRejectsNullListener() {
        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.streamWorkflow(Collections.emptyMap(), "user-1", null));
        assertEquals("流式事件监听器不能为空", exception.getMessage());
    }

    @Test
    void streamWorkflowRejectsUnconfiguredWorkflowCredentials() {
        DifyGatewayImpl unconfigured = newGateway(LEGACY_BASE, LEGACY_KEY, CHATFLOW_BASE, CHATFLOW_KEY, "", WORKFLOW_KEY);

        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> unconfigured.streamWorkflow(Collections.emptyMap(), "user-1", listener()));

        assertEquals("Workflow BASE_URL 未配置", exception.getMessage());
    }

    // ===== exchangeJson 错误处理 =====

    @Test
    void propagatesHttpStatusCodeAsDifyGatewayException() throws Exception {
        HttpStatusCodeException statusException = mock(HttpStatusCodeException.class);
        when(statusException.getRawStatusCode()).thenReturn(429);
        when(statusException.getResponseBodyAsString()).thenReturn("{\"message\":\"rate limited\"}");
        when(statusException.getStatusText()).thenReturn("Too Many Requests");
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(JsonNode.class)))
                .thenThrow(statusException);

        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), "user-1"));

        assertEquals(429, exception.getHttpStatus());
        assertTrue(exception.isRetryable());
        assertTrue(exception.getMessage().contains("rate limited"));
    }

    @Test
    void wrapsResourceAccessExceptionAsRetryable() {
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(JsonNode.class)))
                .thenThrow(new ResourceAccessException("connect timeout"));

        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), "user-1"));

        assertTrue(exception.isRetryable());
        assertTrue(exception.getMessage().contains("无法连接 Dify 服务"));
    }

    @Test
    void rejectsEmptyResponseBody() {
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(null));

        DifyGatewayException exception = assertThrows(DifyGatewayException.class,
                () -> gateway.sendChatMessage(DifyChatApplication.CHATFLOW, chatflowRequest(), "user-1"));

        assertEquals("Chatflow 消息调用返回空响应", exception.getMessage());
    }
}