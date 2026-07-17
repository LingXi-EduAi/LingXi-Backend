package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DifyChatflowServiceImplTests {
    private MockRestServiceServer server;
    private DifyChatflowServiceImpl service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new DifyChatflowServiceImpl(
                restTemplate,
                "http://127.0.0.1/v1/",
                "app-chatflow-test-key"
        );
    }

    @Test
    void sendMessageForwardsAuthenticatedBlockingRequest() {
        server.expect(once(), requestTo("http://127.0.0.1/v1/chat-messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer app-chatflow-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"inputs\":{},\"query\":\"hello\",\"response_mode\":\"blocking\",\"conversation_id\":\"\",\"user\":\"user-1\",\"files\":[],\"auto_generate_name\":true}"))
                .andRespond(withSuccess(
                        "{\"message_id\":\"message-1\",\"conversation_id\":\"conversation-1\",\"answer\":\"world\"}",
                        MediaType.APPLICATION_JSON
                ));

        DifyChatflowRequest request = new DifyChatflowRequest();
        request.setQuery("hello");

        JsonNode response = service.sendMessage(request, "user-1");

        assertEquals("message-1", response.get("message_id").asText());
        assertEquals("world", response.get("answer").asText());
        server.verify();
    }

    @Test
    void getMessagesForwardsAuthenticatedHistoryRequest() {
        server.expect(once(), requestTo("http://127.0.0.1/v1/messages?user=user-1&conversation_id=conversation-1&limit=100"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer app-chatflow-test-key"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":\"message-1\",\"query\":\"hello\",\"answer\":\"world\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        JsonNode response = service.getMessages("conversation-1", "user-1", 100, null);

        assertEquals("hello", response.path("data").get(0).path("query").asText());
        assertEquals("world", response.path("data").get(0).path("answer").asText());
        server.verify();
    }
}
