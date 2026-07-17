package com.lxe.lx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class DifyWorkflowConfig {

    @Bean({"difyRestTemplate", "difyWorkflowRestTemplate"})
    public RestTemplate difyWorkflowRestTemplate(
            RestTemplateBuilder builder,
            @Value("${dify.workflow.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${dify.workflow.read-timeout-ms}") long readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
