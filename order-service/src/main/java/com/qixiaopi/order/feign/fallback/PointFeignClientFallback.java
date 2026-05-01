package com.qixiaopi.order.feign.fallback;

import org.springframework.stereotype.Component;

import com.qixiaopi.order.dto.ResultDTO;
import com.qixiaopi.order.feign.PointFeignClient;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PointFeignClientFallback implements PointFeignClient {

    @Override
    public ResultDTO<String> addPoint(Long userId, Long amount, String orderNo, Integer expireDays) {
        log.warn("积分服务暂时不可用，执行fallback逻辑。用户ID：{}，金额：{}，订单号：{}", userId, amount, orderNo);
        return ResultDTO.failure("积分服务暂时不可用，请稍后重试");
    }
}