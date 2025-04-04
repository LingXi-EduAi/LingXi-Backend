package com.lxe.lx.service.impl;
import com.lxe.lx.service.ApiService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
@Service("ApiService")
public class ApiServiceImpl implements ApiService {
    private static final String API_URL = "http://123.207.27.32/v1/chat-messages";
    private static final String API_KEY = "app-biXDlneU8keswQHN4sgMdxl5"; // 请从安全存储中获取
    private static final ObjectMapper objectMapper = new ObjectMapper(); // 解析 JSON 用

    public String sendMessage(String userInput, String userId, String conversationId) {
        RestTemplate restTemplate = new RestTemplate();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", userInput);
        requestBody.put("inputs", new HashMap<>()); // 这里可以传入额外的变量
        requestBody.put("response_mode", "blocking"); // 或 "blocking"
        requestBody.put("user", userId);
        requestBody.put("conversation_id", conversationId);
        requestBody.put("files", new Object[0]); // 空数组表示没有文件
        requestBody.put("auto_generate_name", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送 POST 请求
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

        return decodeUnicode(response.getBody());
    }

    private String decodeUnicode(String json) {
        try {
            return objectMapper.readTree(json).toString();
        } catch (JsonProcessingException e) {
            return json; // 如果解析失败，返回原始数据
        }
    }
}


