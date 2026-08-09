package com.lxe.lx.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lxe.lx.annotation.Login;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationInterceptorTest {

    private final AuthorizationInterceptor interceptor = new AuthorizationInterceptor();
    private final HandlerMethod securedHandler = new HandlerMethod(
            new SecuredController(),
            SecuredController.class.getDeclaredMethods()[0]);

    @Test
    void aiEndpointMissingTokenIncludesCompleteErrorContract() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, securedHandler));

        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals(401, body.getIntValue("status"));
        assertEquals("参数缺少 token 值", body.getString("msg"));
        assertNotNull(body.getString("requestId"));
        assertFalse(body.getString("requestId").isEmpty());
        assertTrue(body.containsKey("data"));
        assertNull(body.get("data"));
    }

    @Test
    void legacyEndpointMissingTokenKeepsExistingResponseShape() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/detail");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, securedHandler));

        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals(1000, body.getIntValue("status"));
        assertEquals("参数缺少 token 值", body.getString("msg"));
        assertFalse(body.containsKey("requestId"));
        assertFalse(body.containsKey("data"));
    }

    private static class SecuredController {
        @Login
        public void secured() {
        }
    }
}
