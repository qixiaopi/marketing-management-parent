package com.qixiaopi.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service")
public interface PayFeignClient {
    @PostMapping("/account/deduct")
    String deductBalance(@RequestParam("userId") String userId, @RequestParam("amount") Long amount);
}
