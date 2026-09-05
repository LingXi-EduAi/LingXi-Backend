package com.lxe.lx.controller;

import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.TokenService;
import com.lxe.lx.util.MD5;
import com.lxe.lx.util.ResultConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BE-02 回归测试：同一用户再次登录后，旧 token 对应的 Redis key 被删除，
 * 之后旧 token 访问受保护接口会因查不到 key 而返回 401。
 */
class TokenControllerTest {

    private CustomerService customerService;
    private TokenService tokenService;
    private RedisTemplate<String, TokenEntity> redisTemplate;
    private ValueOperations<String, TokenEntity> valueOps;
    private HttpServletRequest request;
    private TokenController controller;

    private final Map<String, TokenEntity> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        customerService = mock(CustomerService.class);
        tokenService = mock(TokenService.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.keys(anyString())).thenAnswer(inv -> (Set<String>) store.keySet());
        doAnswer(inv -> {
                    store.put(inv.getArgument(0), inv.getArgument(1));
                    return null;
                }).when(valueOps).set(anyString(), any(TokenEntity.class), anyLong(), any(TimeUnit.class));
        when(valueOps.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> store.remove(inv.getArgument(0)) != null);

        controller = new TokenController();
        ReflectionTestUtils.setField(controller, "customerService", customerService);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);
    }

    private void stubUser(String userId, String id, String rawPassword) throws Exception {
        when(customerService.getCustomerByUserId(anyString())).thenReturn(null);
        when(customerService.getCustomerByEmail(anyString())).thenReturn(null);
        when(customerService.getCustomerByPhoneNumber(anyString())).thenReturn(null);
        Customer repo = new Customer();
        repo.setUserId(userId);
        repo.setPassword(MD5.md5(rawPassword));
        repo.setId(id);
        repo.setState("1");
        when(customerService.getCustomerByUserId(userId)).thenReturn(repo);
    }

    private String login(String userId, String id, String rawPassword) throws Exception {
        stubUser(userId, id, rawPassword);
        Customer body = new Customer();
        body.setUserId(userId);
        body.setPassword(rawPassword);
        ResultConstant result = controller.login(request, body);
        return (String) result.getData();
    }

    @Test
    void secondLoginDeletesOldTokenKey() throws Exception {
        String oldToken = login("stu01", "id-stu01", "pass123");

        assertTrue(store.containsKey("token:" + oldToken), "登录后应写入 token key");

        String newToken = login("stu01", "id-stu01", "pass123");

        assertFalse(store.containsKey("token:" + oldToken), "二次登录后旧 token key 应被删除");
        assertTrue(store.containsKey("token:" + newToken), "新 token key 应存在");
        assertNotSame(oldToken, newToken);
    }
}
