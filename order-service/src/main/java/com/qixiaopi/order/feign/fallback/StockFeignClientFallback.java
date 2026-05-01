package com.qixiaopi.order.feign.fallback;

import org.springframework.stereotype.Component;

import com.qixiaopi.order.dto.ResultDTO;
import com.qixiaopi.order.feign.StockFeignClient;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StockFeignClientFallback implements StockFeignClient {
    @Override
    public ResultDTO<Boolean> deductStock(Long skuId, Integer num, String orderNo) {
        log.warn("库存服务暂时不可用，执行fallback逻辑。商品ID：{}，数量：{}，订单号：{}", skuId, num, orderNo);
        return ResultDTO.failure("库存服务暂时不可用，请稍后重试");
    }
}