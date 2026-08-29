package com.lxe.lx.config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.domain.qo.TokenQO;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.TokenService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    public final static String ORG_ID_KEY = "orgId";

    @Autowired
    private RedisTemplate<String, TokenEntity> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Login login = null;
        TeacherOnly teacherOnly = null;
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            login = handlerMethod.getMethodAnnotation(Login.class);
            teacherOnly = handlerMethod.getMethodAnnotation(TeacherOnly.class);
            if (teacherOnly == null) {
                teacherOnly = handlerMethod.getBeanType().getAnnotation(TeacherOnly.class);
            }
            if (login == null && teacherOnly == null) {
                return true;
            }
        } else {
            if (!request.getRequestURI().startsWith("/uploadFiles/file/")) {
                return true;
            }
        }

        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        if (StringUtils.isBlank(token)) {
            return sendErrorResponse(request, response, "参数缺少 token 值");
        }

        // 🚀 使用 Redis 查询 TokenEntity
//        TokenEntity tokenEntity = redisTemplate.opsForValue().get("token:" + token);
        Object obj = redisTemplate.opsForValue().get("token:" + token);
        if (obj == null) {
            return sendErrorResponse(request, response, "登录过期，请重新登录");
        }
        TokenEntity tokenEntity = new TokenEntity();
        if (obj instanceof LinkedHashMap) {
            ObjectMapper objectMapper = new ObjectMapper();
            tokenEntity = objectMapper.convertValue(obj, TokenEntity.class);
        } else if (obj instanceof TokenEntity) {
            tokenEntity = (TokenEntity) obj;
        }else {
            return sendErrorResponse(request, response, "登录状态异常，请重新登录");
        }

        if (isTokenEntityInvalid(tokenEntity)) {
            return sendErrorResponse(request, response, "登录过期，请重新登录");
        }

        if (teacherOnly != null && !TokenEntity.ROLE_TEACHER.equals(tokenEntity.getRole())) {
            return sendForbiddenResponse(response);
        }

        request.setAttribute(ORG_ID_KEY, tokenEntity);
        return true;
    }

    private boolean sendErrorResponse(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        PrintWriter out = response.getWriter();
        if (request.getRequestURI().startsWith("/api/ai/tasks")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("msg", message);
            body.put("requestId", UUID.randomUUID().toString().replace("-", ""));
            body.put("data", null);
            out.write(JSON.toJSONString(body, SerializerFeature.WriteMapNullValue));
        } else {
            out.write(JSON.toJSONString(ResultConstant.init(ResultConstant.TOKEN_INVALID, message, null)));
        }
        out.flush();
        out.close();
        return false;
    }

    private boolean sendForbiddenResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(ResultConstant.notAuthorized()));
        out.flush();
        out.close();
        return false;
    }

    private boolean isTokenEntityInvalid(TokenEntity tokenEntity) {
        return tokenEntity == null ||
                tokenEntity.getId() == null ||
                tokenEntity.getToken() == null ||
                tokenEntity.getUserId() == null ||
                tokenEntity.getName() == null ||
                tokenEntity.getUpdateTime() == null ||
                tokenEntity.getIp() == null ||
                tokenEntity.getState() == null;
    }
}
