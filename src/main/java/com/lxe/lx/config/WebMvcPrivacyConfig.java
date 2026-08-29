package com.lxe.lx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 隐私与安全拦截器注册（BE-14）。
 *
 * <p>为 {@code /api/ai/**} 注册访问审计与限流拦截器。
 * 两者均独立于 {@link AuthorizationInterceptor}，不修改其行为；
 * 通过 {@code order} 保证在鉴权放行之后执行，因此不会影响登录链路。
 */
@Configuration
public class WebMvcPrivacyConfig implements WebMvcConfigurer {

    private final AiAuditInterceptor aiAuditInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcPrivacyConfig(AiAuditInterceptor aiAuditInterceptor,
                               RateLimitInterceptor rateLimitInterceptor) {
        this.aiAuditInterceptor = aiAuditInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 鉴权（AuthorizationInterceptor，order 默认 0）之后执行
        registry.addInterceptor(aiAuditInterceptor)
                .addPathPatterns("/api/ai/**")
                .order(1);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/**")
                .order(2);
    }
}
