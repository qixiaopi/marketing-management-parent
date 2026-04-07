package com.qixiaopi.order.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        List<InnerInterceptor> innerInterceptors = new ArrayList<>();
        
        // 添加动态表名插件
        DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
        // 对于 MyBatis Plus 3.x，使用 setTableNameHandler 方法
        dynamicTableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> {
            if ("t_order".equals(tableName)) {
                Long userId = DynamicTableNameContext.getUserId();
                if (userId != null) {
                    return "t_order_" + (userId % 3);
                }
            }
            return tableName;
        });
        innerInterceptors.add(dynamicTableNameInnerInterceptor);
        
        interceptor.setInterceptors(innerInterceptors);
        
        return interceptor;
    }
}
