package com.qixiaopi.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.qixiaopi.order.feign.fallback.PayFeignClientFallback;
import com.qixiaopi.order.dto.ResultDTO;

@FeignClient(name = "account-service", fallback = PayFeignClientFallback.class, configuration = com.qixiaopi.order.config.FeignConfig.class)
public interface PayFeignClient {
    @PostMapping("/account/deduct")
    ResultDTO<String> deductBalance(@RequestParam("userId") String userId, @RequestParam("amount") Long amount, @RequestParam("orderNo") String orderNo);
}
