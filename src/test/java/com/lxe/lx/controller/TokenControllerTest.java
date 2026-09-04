package com.lxe.lx.controller;

import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenControllerTest {

    @Test
    void secondLoginEvictsPreviousUserTokenBeforeSavingNewToken() throws Exception {
        CustomerService customerService = mock(CustomerService.class);
        RedisTemplate<String, TokenEntity> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, TokenEntity> operations = mock(ValueOperations.class);
        TokenController controller = new TokenController();

        Customer customer = new Customer();
        customer.setId("customer-1");
        customer.setUserId("student114514");
        customer.setName("Student");
        customer.setState(TokenEntity.ROLE_STUDENT);
        customer.setPassword(com.lxe.lx.util.MD5.md5("123456"));
        when(customerService.getCustomerByUserId(customer.getUserId())).thenReturn(customer);
        when(redisTemplate.opsForValue()).thenReturn(operations);

        TokenEntity previous = new TokenEntity();
        previous.setToken("old-token");
        when(operations.get("user-token:" + customer.getUserId())).thenReturn(previous);
        when(redisTemplate.delete("token:old-token")).thenReturn(true);

        ReflectionTestUtils.setField(controller, "customerService", customerService);
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        com.lxe.lx.util.ResultConstant result = controller.login(request, customerRequest());

        assertEquals(200, result.getStatus());
        assertTrue(result.getData() instanceof String);
        String newToken = (String) result.getData();
        verify(redisTemplate).delete("token:old-token");
        verify(operations).set(eq("token:" + newToken), any(TokenEntity.class), eq(30L), eq(TimeUnit.MINUTES));
        verify(operations).set(eq("user-token:student114514"), any(TokenEntity.class), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void firstLoginDoesNotDeleteNewlyGeneratedToken() throws Exception {
        CustomerService customerService = mock(CustomerService.class);
        RedisTemplate<String, TokenEntity> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, TokenEntity> operations = mock(ValueOperations.class);
        TokenController controller = new TokenController();

        Customer customer = new Customer();
        customer.setId("customer-1");
        customer.setUserId("student114514");
        customer.setName("Student");
        customer.setState(TokenEntity.ROLE_STUDENT);
        customer.setPassword(com.lxe.lx.util.MD5.md5("123456"));
        when(customerService.getCustomerByUserId(customer.getUserId())).thenReturn(customer);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("user-token:" + customer.getUserId())).thenReturn(null);

        ReflectionTestUtils.setField(controller, "customerService", customerService);
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        com.lxe.lx.util.ResultConstant result = controller.login(request, customerRequest());

        assertEquals(200, result.getStatus());
        verify(redisTemplate, never()).delete(any(String.class));
    }

    private Customer customerRequest() {
        Customer request = new Customer();
        request.setUserId("student114514");
        request.setPassword("123456");
        return request;
    }
}
