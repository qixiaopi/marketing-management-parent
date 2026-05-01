package com.qixiaopi.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.qixiaopi.order.dto.ResultDTO;
import com.qixiaopi.order.feign.fallback.StockFeignClientFallback;

@FeignClient(name = "inventory-service", fallback = StockFeignClientFallback.class, configuration = com.qixiaopi.order.config.FeignConfig.class)
public interface StockFeignClient {
    @PostMapping("/inventory/deductStock")
    ResultDTO<Boolean> deductStock(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, @RequestParam("orderNo") String orderNo);
}
