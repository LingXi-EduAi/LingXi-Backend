package com.lxe.lx.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcTokenConfig implements WebMvcConfigurer {
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/customer/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/LXClass/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/token/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/classGrouping/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/conversation/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/document/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/homework/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/upload/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/grade/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/studyGroup/**");

    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                //是否发送Cookie
                .allowCredentials(true)
                //放行哪些原始域
                .allowedOriginPatterns("*")
                .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"})
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}