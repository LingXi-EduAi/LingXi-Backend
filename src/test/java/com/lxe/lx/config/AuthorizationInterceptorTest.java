package com.lxe.lx.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.pojo.TokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AuthorizationInterceptor 单元测试（纯 JUnit 5 + Mockito，无 Spring 上下文）。
 *
 * <p>拦截器的 {@code redisTemplate} 是 {@code @Autowired} 字段注入，测试通过反射注入
 * Mockito mock，覆盖：token header 校验、Redis 查询结果分支（null / LinkedHashMap /
 * TokenEntity / 非法类型）、{@code @TeacherOnly} 角色校验、{@code ?token=} 参数回退。</p>
 */
class AuthorizationInterceptorTest {

    private AuthorizationInterceptor interceptor;
    private RedisTemplate<String, TokenEntity> redisTemplate;
    private ValueOperations<String, TokenEntity> valueOps;
    private HandlerMethod securedHandler;
    private HandlerMethod teacherOnlyHandler;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new AuthorizationInterceptor();
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Field redisField = AuthorizationInterceptor.class.getDeclaredField("redisTemplate");
        redisField.setAccessible(true);
        redisField.set(interceptor, redisTemplate);

        securedHandler = new HandlerMethod(new SecuredController(), SecuredController.class.getMethod("secured"));
        teacherOnlyHandler = new HandlerMethod(new SecuredController(), SecuredController.class.getMethod("teacherOnly"));
    }

    private TokenEntity teacherToken() {
        TokenEntity entity = new TokenEntity();
        entity.setId("id-1");
        entity.setToken("token-abc");
        entity.setUserId("user-1");
        entity.setName("张老师");
        entity.setUpdateTime("2026-09-05 10:00:00");
        entity.setIp("127.0.0.1");
        entity.setState("1");
        entity.setRole(TokenEntity.ROLE_TEACHER);
        return entity;
    }

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

    @Test
    void validTeacherTokenWithoutTeacherOnlyProceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenEntity token = teacherToken();
        when(valueOps.get("token:token-abc")).thenReturn(token);

        assertTrue(interceptor.preHandle(request, response, securedHandler));

        assertEquals(200, response.getStatus());
        assertSame(token, request.getAttribute(AuthorizationInterceptor.ORG_ID_KEY));
    }

    @Test
    void expiredTokenReturns401LoginExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOps.get("token:token-abc")).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, securedHandler));

        assertEquals(401, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals("登录过期，请重新登录", body.getString("msg"));
    }

    @Test
    void malformedRedisValueReturns401LoginStateAbnormal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 普通 HashMap 既不是 LinkedHashMap 也不是 TokenEntity → 走"登录状态异常"分支
        doReturn(new HashMap<>()).when(valueOps).get("token:token-abc");

        assertFalse(interceptor.preHandle(request, response, securedHandler));

        assertEquals(401, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals("登录状态异常，请重新登录", body.getString("msg"));
    }

    @Test
    void linkedHashMapRedisValueIsConvertedToTokenEntity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "id-1");
        map.put("token", "token-abc");
        map.put("userId", "user-1");
        map.put("name", "张老师");
        map.put("updateTime", "2026-09-05 10:00:00");
        map.put("ip", "127.0.0.1");
        map.put("state", "1");
        map.put("role", "1");
        doReturn(map).when(valueOps).get("token:token-abc");

        assertTrue(interceptor.preHandle(request, response, securedHandler));

        TokenEntity converted = (TokenEntity) request.getAttribute(AuthorizationInterceptor.ORG_ID_KEY);
        assertEquals("user-1", converted.getUserId());
        assertEquals(TokenEntity.ROLE_TEACHER, converted.getRole());
    }

    @Test
    void incompleteTokenEntityReturns401LoginExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenEntity incomplete = teacherToken();
        incomplete.setUserId(null);
        when(valueOps.get("token:token-abc")).thenReturn(incomplete);

        assertFalse(interceptor.preHandle(request, response, securedHandler));

        assertEquals(401, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals("登录过期，请重新登录", body.getString("msg"));
    }

    @Test
    void studentTokenOnTeacherOnlyMethodReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/teacher/class");
        request.addHeader("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenEntity student = teacherToken();
        student.setRole(TokenEntity.ROLE_STUDENT);
        when(valueOps.get("token:token-abc")).thenReturn(student);

        assertFalse(interceptor.preHandle(request, response, teacherOnlyHandler));

        assertEquals(403, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(1100, body.getIntValue("status"));
        assertEquals("账号未授权", body.getString("msg"));
    }

    @Test
    void tokenQueryParameterFallbackStillValidates() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/tasks/task-1");
        request.addParameter("token", "token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenEntity token = teacherToken();
        when(valueOps.get("token:token-abc")).thenReturn(token);

        assertTrue(interceptor.preHandle(request, response, securedHandler));

        assertSame(token, request.getAttribute(AuthorizationInterceptor.ORG_ID_KEY));
    }

    private static class SecuredController {
        @Login
        public void secured() {
        }

        @Login
        @TeacherOnly
        public void teacherOnly() {
        }
    }
}