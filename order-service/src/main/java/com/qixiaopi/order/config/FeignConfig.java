package com.qixiaopi.order.config;

import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor seataFeignInterceptor() {
        return template -> {
            // 先尝试从ThreadLocal获取XID
            String xid = RootContext.getXID();
            log.info("Feign拦截器从ThreadLocal获取到XID: {}", xid);
            
            // 如果ThreadLocal中没有，尝试从系统属性获取
            if (xid == null) {
                xid = System.getProperty("seata.xid");
                log.info("Feign拦截器从系统属性获取到XID: {}", xid);
            }
            
            // 如果获取到XID，设置到请求头
            if (xid != null) {
                template.header(RootContext.KEY_XID, xid);
                log.info("Feign拦截器设置XID到请求头: {}", xid);
            } else {
                log.warn("Feign拦截器未获取到XID");
            }
        };
    }
}