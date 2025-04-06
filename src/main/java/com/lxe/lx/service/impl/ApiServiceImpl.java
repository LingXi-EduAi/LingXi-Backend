package com.lxe.lx.service.impl;
import com.lxe.lx.service.ApiService;
import com.lxe.lx.util.Tools;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;

@Service("ApiService")
public class ApiServiceImpl implements ApiService {
    private static final String API_URL = "http://123.207.27.32/v1/chat-messages";
    private static final String API_KEY = "app-biXDlneU8keswQHN4sgMdxl5"; // 请从安全存储中获取

    private static final String UPLOAD_URL = "http://123.207.27.32/v1/files/upload";
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

        return Tools.decodeUnicode(response.getBody());
    }

    public String fileUpload(MultipartFile file, String id) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 1. 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // 保持原始文件名
                }
            };
            body.add("file",fileAsResource);
            body.add("user", id);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(UPLOAD_URL, HttpMethod.POST, requestEntity, String.class);
            System.out.println(Tools.decodeUnicode(response.getBody()));
            return Tools.decodeUnicode(response.getBody()); // 返回服务器响应
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
    public String renameConversation(String conversationId, String userId, String newName, boolean autoGenerate){
        String url = "http://123.207.27.32/v1/conversations/" + conversationId + "/name";
        // 构造请求体
        Map<String, Object> body = new HashMap<>();
        if (!autoGenerate) {
            body.put("name", newName);
        }
        body.put("auto_generate", autoGenerate);
        body.put("user", userId);

        // 构造请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY); // 替换为你项目中获取 key 的方式

        // 构造请求实体
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 发送 POST 请求
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//        Tools.decodeUnicode(response.getBody());
        return Tools.decodeUnicode(response.getBody());  // 返回完整 JSON 字符串
    }
    public String audioToText(MultipartFile file, String id){
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + API_KEY);
            // 构造 multipart 请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // 保持原始文件名
                }
            };
            body.add("file", fileAsResource);
            body.add("user", id);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity("http://123.207.27.32/v1/audio-to-text", requestEntity, String.class);
            return Tools.decodeUnicode(response.getBody());
        }catch (Exception e) {
            return "上传失败：" + e.getMessage();
        }
    }

}


