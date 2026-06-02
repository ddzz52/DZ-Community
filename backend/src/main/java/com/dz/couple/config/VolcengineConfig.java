package com.dz.couple.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 火山引擎 Ark API 配置类 — Bean 定义
 * 属性绑定在 VolcengineProperties 中，确保 @Bean 执行时属性已就绪
 */
@Configuration
@EnableConfigurationProperties(VolcengineProperties.class)
public class VolcengineConfig {

    private final VolcengineProperties properties;

    public VolcengineConfig(VolcengineProperties properties) {
        this.properties = properties;
    }

    @Primary
    @Bean
    public RestTemplate volcengineRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeout());
        factory.setReadTimeout(properties.getTimeout());
        return new RestTemplate(factory);
    }
}
