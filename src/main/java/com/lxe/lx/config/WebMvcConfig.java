package com.lxe.lx.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一 Web MVC 配置：合并原 WebMvcConfig/WebMvcTokenConfig/WebMvcPrivacyConfig。
 * 注册顺序：鉴权(0) -> AI 审计(1) -> 限流(2)。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    private final AiAuditInterceptor aiAuditInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(AiAuditInterceptor aiAuditInterceptor,
                        RateLimitInterceptor rateLimitInterceptor) {
        this.aiAuditInterceptor = aiAuditInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
        registry.addInterceptor(aiAuditInterceptor)
                .addPathPatterns("/api/ai/**")
                .order(1);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/**")
                .order(2);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"})
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // registry.addResourceHandler("/uploadFilesTest/file/**").addResourceLocations("file:/home/server/eaiap/uploadFilesTest/file/");
        // registry.addResourceHandler("/uploadFilesTest/file/**").addResourceLocations("file:D:/uploadFilesTest/file/");
    }
}