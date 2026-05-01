package com.qixiaopi.order.feign;

import com.qixiaopi.order.dto.ResultDTO;
import com.qixiaopi.order.feign.fallback.PointFeignClientFallback;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "point-service", fallback = PointFeignClientFallback.class, configuration = com.qixiaopi.order.config.FeignConfig.class)
public interface PointFeignClient {
    @PostMapping("/point/add")
    ResultDTO<String> addPoint(@RequestParam("userId") Long userId, @RequestParam("amount") Long amount, @RequestParam("orderNo") String orderNo, @RequestParam(value = "expireDays", required = false) Integer expireDays);
}
