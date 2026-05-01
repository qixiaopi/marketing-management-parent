package com.qixiaopi.order.feign.fallback;

import org.springframework.stereotype.Component;

import com.qixiaopi.order.feign.PayFeignClient;
import com.qixiaopi.order.dto.ResultDTO;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PayFeignClientFallback implements PayFeignClient {
    @Override
    public ResultDTO<String> deductBalance(String userId, Long amount, String orderNo) {
        log.warn("支付服务暂时不可用，执行fallback逻辑。用户ID：{}，金额：{}，订单号：{}", userId, amount, orderNo);
        return ResultDTO.failure("支付服务暂时不可用，请稍后重试");
    }
}