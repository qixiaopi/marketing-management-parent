package com.qixiaopi.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ElasticsearchConfig {
    
    @Value("${spring.elasticsearch.connection-timeout:5000}")
    private int connectionTimeout;
    
    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private int socketTimeout;

    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(socketTimeout);
        return new RestTemplate(factory);
    }
}
