package com.lxe.lx.config;

import com.alibaba.fastjson.JSON;
import com.lxe.lx.annotation.RateLimit;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.ratelimit.RateLimitService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 接口限流拦截器。
 *
 * <p>对标注了 {@link RateLimit} 的接口按用户 + 路径 + 时间窗口限流，
 * 超过阈值返回 HTTP 429 + JSON 错误。
 * 独立于 {@link AuthorizationInterceptor}，仅在已登录放行后执行，
 * 且仅注册到 {@code /api/ai/**}，不会影响登录链路。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        Object attr = request.getAttribute(AuthorizationInterceptor.ORG_ID_KEY);
        String userId = null;
        if (attr instanceof TokenEntity) {
            userId = ((TokenEntity) attr).getId();
        }

        boolean allowed = rateLimitService.tryAcquire(
                userId, request.getRequestURI(), rateLimit.limit(), rateLimit.windowSeconds());
        if (allowed) {
            return true;
        }
        sendTooManyRequests(response);
        return false;
    }

    private void sendTooManyRequests(HttpServletResponse response) throws Exception {
        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 429);
        body.put("msg", "请求过于频繁，请稍后再试");
        body.put("requestId", UUID.randomUUID().toString().replace("-", ""));
        body.put("data", null);
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(body));
        out.flush();
        out.close();
    }
}
