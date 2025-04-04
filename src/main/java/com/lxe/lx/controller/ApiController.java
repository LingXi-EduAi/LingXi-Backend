package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.ApiService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ApiService apiService;
    @Login
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public ResultConstant test(HttpServletRequest request) throws Exception {
        String response = apiService.sendMessage("介绍一下你自己", "user_123",null);
        System.out.println(response);
        return ResultConstant.success(response);
    }
    @Login
    @RequestMapping(value = "/chatMessage", method = RequestMethod.POST)
    public ResultConstant test(HttpServletRequest request,String query,String conversationId) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        String response = apiService.sendMessage(query, tokenEntity.getId(),conversationId);
        System.out.println(response);
        return ResultConstant.success(response);
    }
}
