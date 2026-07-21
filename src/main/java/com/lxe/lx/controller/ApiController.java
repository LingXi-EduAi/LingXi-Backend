package com.lxe.lx.controller;
import com.lxe.lx.pojo.Conversation;
import com.lxe.lx.service.ConversationService;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.ApiService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ApiService apiService;
    @Autowired
    private ConversationService conversationService;
    @Value("${api.base-url}")
    private String apiBaseUrl;
    @Value("${api.key}")
    private String apiKey;
    @Login
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public ResultConstant test(HttpServletRequest request) throws Exception {
        String response = apiService.sendMessage("介绍一下你自己", "user_123",null);
        System.out.println(response);
        return ResultConstant.success(response);
    }
    @Login
    @RequestMapping(value = "/chatMessage", method = RequestMethod.POST)
    public ResultConstant chatMessage(HttpServletRequest request,String query,Conversation conversation) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        String response = apiService.sendMessage(query, tokenEntity.getId(),conversation.getConversationId());
        System.out.println(response);
        JSONObject jsonObject = new JSONObject(response);
        if(StringUtils.isBlank(conversation.getConversationId())) {
            String conversationIdTemp = jsonObject.getString("conversation_id");
            conversation.setId(Tools.get32UUID());
            conversation.setConversationId(conversationIdTemp);
            conversation.setStudentId(tokenEntity.getId());
            conversation.setCreateId(tokenEntity.getId());
            conversation.setCreateTime(Tools.nowTimeStr());
            conversation.setState("1");
            conversation.setVersion(1);
            ResultConstant ref = conversationService.add(conversation);
        }
        return ResultConstant.success(response);
    }
    @Login
    @RequestMapping(value = "/filesUpload", method = RequestMethod.POST)
    public ResultConstant filesUpload(HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        String ref = apiService.fileUpload(file,tokenEntity.getId());
        return ResultConstant.success(ref);
    }
    @Login
    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    public ResultConstant messages(HttpServletRequest request, String conversationId,@RequestParam(value = "limit", required = false, defaultValue = "20")int limit,String firstId) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl("/messages"))
                .queryParam("user", tokenEntity.getId())
                .queryParam("conversation_id", conversationId)
                .queryParam("limit", limit);
        if (StringUtils.isNotBlank(firstId)) {
            urlBuilder.queryParam("first_id", firstId);
        }
        String url = urlBuilder.build().encode().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        System.out.println(Tools.decodeUnicode(response.getBody()));
        // 返回封装的结果
        return ResultConstant.success(Tools.decodeUnicode(response.getBody()));
    }
    @Login
    @RequestMapping(value = "/conversations", method = RequestMethod.POST)
    public ResultConstant conversations(HttpServletRequest request,
                                        @RequestParam(value = "last_id", required = false) String lastId,
                                        @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
                                        @RequestParam(value = "sort_by", required = false, defaultValue = "-updated_at") String sortBy) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl("/conversations"))
                .queryParam("user", tokenEntity.getId())
                .queryParam("limit", limit)
                .queryParam("sort_by", sortBy);
        if (StringUtils.isNotBlank(lastId)) {
            urlBuilder.queryParam("last_id", lastId);
        }
        String url = urlBuilder.build().encode().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        System.out.println(Tools.decodeUnicode(response.getBody()));
        // 返回结果（String JSON）
        return ResultConstant.success(Tools.decodeUnicode(response.getBody()));
    }
    @Login
    @RequestMapping(value = "/deleteConversation", method = RequestMethod.POST)
    public ResultConstant deleteConversation(HttpServletRequest request,
                                             String conversationId) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        Conversation temp = conversationService.getConversationByConversationId(conversationId);
        if(temp==null){
            return ResultConstant.error("会话id有误");
        }
        // 请求地址
        String url = apiUrl("/conversations/" + conversationId);

        // 设置请求体（JSON 格式）
        Map<String, String> body = new HashMap<>();
        body.put("user", tokenEntity.getId());

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建请求实体
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        // 发起 DELETE 请求
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        conversationService.deleteByConversationId(conversationId);
        // 返回响应结果
        return ResultConstant.success(response.getBody());  // 返回的是 {"result":"success"}
    }

    @Login
    @RequestMapping(value = "/renameConversation", method = RequestMethod.POST)
    public ResultConstant renameConversation(HttpServletRequest request,
                                             @RequestParam String conversationId,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(defaultValue = "false") boolean autoGenerate) {
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            String result = apiService.renameConversation(conversationId, tokenEntity.getId(), name, autoGenerate);
            return ResultConstant.success(result);
        } catch (Exception e) {
            return ResultConstant.error("会话重命名失败：" + e.getMessage());
        }
    }

    private String apiUrl(String path) {
        return UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path(path)
                .build()
                .toUriString();
    }
//    @Login
//    @RequestMapping(value = "/audioToText", method = RequestMethod.POST)
//    public ResultConstant audioToText(HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
//        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
//        String ref = apiService.audioToText(file,tokenEntity.getId());
//        return ResultConstant.success(ref);
//    }




}
