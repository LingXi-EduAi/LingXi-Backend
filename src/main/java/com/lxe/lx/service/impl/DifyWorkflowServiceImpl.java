package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.service.DifyWorkflowService;
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

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyWorkflowServiceImpl implements DifyWorkflowService {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String workflowUrl;
    private final String apiKey;

    public DifyWorkflowServiceImpl(
            @Qualifier("difyWorkflowRestTemplate") RestTemplate restTemplate,
            @Value("${dify.workflow.base-url}") String baseUrl,
            @Value("${dify.workflow.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.workflowUrl = StringUtils.removeEnd(baseUrl, "/") + "/workflows/run";
        this.apiKey = apiKey;
    }

    @Override
    public JsonNode runWorkflow(Map<String, Object> inputs, String userId) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalStateException("DIFY_WORKFLOW_BASE_URL 未配置");
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("DIFY_WORKFLOW_API_KEY 未配置");
        }
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("Dify user 不能为空");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs == null ? new HashMap<>() : inputs);
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", userId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                workflowUrl,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("Dify 工作流返回空响应");
        }
        return response.getBody();
    }
}
