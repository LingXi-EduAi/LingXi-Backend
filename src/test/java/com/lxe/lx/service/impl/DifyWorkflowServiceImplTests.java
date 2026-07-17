package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DifyWorkflowServiceImplTests {
    private MockRestServiceServer server;
    private DifyWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new DifyWorkflowServiceImpl(
                restTemplate,
                "http://127.0.0.1/v1/",
                "app-test-key"
        );
    }

    @Test
    void runWorkflowForwardsAuthenticatedBlockingRequest() {
        server.expect(once(), requestTo("http://127.0.0.1/v1/workflows/run"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer app-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"inputs\":{\"question\":\"test\"},\"response_mode\":\"blocking\",\"user\":\"user-1\"}"))
                .andRespond(withSuccess(
                        "{\"workflow_run_id\":\"run-1\",\"data\":{\"status\":\"succeeded\",\"outputs\":{\"answer\":\"ok\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", "test");

        JsonNode response = service.runWorkflow(inputs, "user-1");

        assertEquals("run-1", response.get("workflow_run_id").asText());
        assertEquals("ok", response.path("data").path("outputs").path("answer").asText());
        server.verify();
    }

    @Test
    void runWorkflowRejectsBlankApiKey() {
        DifyWorkflowServiceImpl serviceWithoutKey = new DifyWorkflowServiceImpl(
                new RestTemplate(),
                "http://127.0.0.1/v1",
                ""
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serviceWithoutKey.runWorkflow(new HashMap<>(), "user-1")
        );

        assertEquals("DIFY_WORKFLOW_API_KEY 未配置", exception.getMessage());
    }
}
