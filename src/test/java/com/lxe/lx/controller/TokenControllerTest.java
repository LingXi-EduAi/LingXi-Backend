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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BE-02 回归：同一账号只保留最新 Redis Token。 */
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
        when(redisTemplate.keys(anyString())).thenAnswer(inv -> {
            String pattern = inv.getArgument(0);
            Set<String> matching = new java.util.HashSet<>();
            for (String key : store.keySet()) {
                if ("token:*".equals(pattern) && key.startsWith("token:")) {
                    matching.add(key);
                }
            }
            return matching;
        });
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

    @Test
    void secondLoginDeletesOldTokenAndReplacesUserIndex() throws Exception {
        String oldToken = login("stu01", "id-stu01", "pass123");
        assertTrue(store.containsKey("token:" + oldToken));
        assertTrue(store.containsKey("user-token:stu01"));

        String newToken = login("stu01", "id-stu01", "pass123");

        assertFalse(store.containsKey("token:" + oldToken));
        assertTrue(store.containsKey("token:" + newToken));
        assertNotSame(oldToken, newToken);
        assertNotNull(store.get("user-token:stu01"));
        assertTrue(newToken.equals(store.get("user-token:stu01").getToken()));
    }

    @Test
    void firstLoginDoesNotDeleteAnyToken() throws Exception {
        String token = login("new-user", "id-new", "pass123");
        assertTrue(store.containsKey("token:" + token));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void legacyTokenWithoutUserIndexIsAlsoInvalidated() throws Exception {
        TokenEntity legacy = new TokenEntity();
        legacy.setToken("legacy-token");
        legacy.setUserId("stu01");
        legacy.setId("id-stu01");
        store.put("token:legacy-token", legacy);

        String newToken = login("stu01", "id-stu01", "pass123");

        assertFalse(store.containsKey("token:legacy-token"));
        assertTrue(store.containsKey("token:" + newToken));
    }

    private String login(String userId, String id, String rawPassword) throws Exception {
        Customer repo = new Customer();
        repo.setUserId(userId);
        repo.setPassword(MD5.md5(rawPassword));
        repo.setId(id);
        repo.setName("Test User");
        repo.setState(TokenEntity.ROLE_STUDENT);
        when(customerService.getCustomerByUserId(userId)).thenReturn(repo);
        Customer body = new Customer();
        body.setUserId(userId);
        body.setPassword(rawPassword);
        ResultConstant result = controller.login(request, body);
        assertTrue(result.getStatus() == ResultConstant.SUCCESS);
        return (String) result.getData();
    }
}
