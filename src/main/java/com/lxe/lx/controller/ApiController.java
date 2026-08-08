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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ApiService apiService;
    @Autowired
    private ConversationService conversationService;
    @Login
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public ResultConstant test(HttpServletRequest request) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        String response = apiService.sendMessage("介绍一下你自己", tokenEntity.getId(),null);
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
        String response = apiService.getMessages(
                conversationId,
                tokenEntity.getId(),
                limit,
                firstId
        );
        return ResultConstant.success(response);
    }
    @Login
    @RequestMapping(value = "/conversations", method = RequestMethod.POST)
    public ResultConstant conversations(HttpServletRequest request,
                                        @RequestParam(value = "last_id", required = false) String lastId,
                                        @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
                                        @RequestParam(value = "sort_by", required = false, defaultValue = "-updated_at") String sortBy) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        String response = apiService.getConversations(tokenEntity.getId(), lastId, limit, sortBy);
        return ResultConstant.success(response);
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
        if (!tokenEntity.getId().equals(temp.getStudentId())) {
            return ResultConstant.notAuthorized();
        }
        String response = apiService.deleteConversation(conversationId, tokenEntity.getId());
        conversationService.deleteByConversationId(conversationId);
        return ResultConstant.success(response);
    }

    @Login
    @RequestMapping(value = "/renameConversation", method = RequestMethod.POST)
    public ResultConstant renameConversation(HttpServletRequest request,
                                             @RequestParam String conversationId,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(defaultValue = "false") boolean autoGenerate) {
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Conversation conversation = conversationService.getConversationByConversationId(conversationId);
            if (conversation != null && !tokenEntity.getId().equals(conversation.getStudentId())) {
                return ResultConstant.notAuthorized();
            }
            String result = apiService.renameConversation(conversationId, tokenEntity.getId(), name, autoGenerate);
            return ResultConstant.success(result);
        } catch (Exception e) {
            return ResultConstant.error("会话重命名失败：" + e.getMessage());
        }
    }

//    @Login
//    @RequestMapping(value = "/audioToText", method = RequestMethod.POST)
//    public ResultConstant audioToText(HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
//        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
//        String ref = apiService.audioToText(file,tokenEntity.getId());
//        return ResultConstant.success(ref);
//    }




}
