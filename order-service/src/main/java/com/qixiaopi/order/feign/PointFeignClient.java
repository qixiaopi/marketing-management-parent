package com.qixiaopi.order.feign;

import com.qixiaopi.order.dto.ResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "point-service")
public interface PointFeignClient {
    @PostMapping("/point/add")
    ResultDTO<Boolean> addPoint(@RequestParam("userId") Long userId, @RequestParam("amount") Integer amount);
}
