package com.lxe.lx.config;

import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiAuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * AI 访问审计拦截器。
 *
 * <p>对 {@code /api/ai/**} 的访问记录审计日志（用户、路径、方法、IP、时间）。
 * 独立于 {@link AuthorizationInterceptor}，不修改其行为。
 * 仅在 {@link AuthorizationInterceptor} 放行（已登录）后执行，因此不会影响登录链路。
 */
@Component
public class AiAuditInterceptor implements HandlerInterceptor {

    private final AiAuditLogService auditLogService;

    public AiAuditInterceptor(AiAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attr = request.getAttribute(AuthorizationInterceptor.ORG_ID_KEY);
        if (attr instanceof TokenEntity) {
            TokenEntity token = (TokenEntity) attr;
            auditLogService.record(
                    token.getId(),
                    request.getRequestURI(),
                    request.getMethod(),
                    clientIp(request));
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
