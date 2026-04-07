package com.qixiaopi.product.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign 客户端
 */
@FeignClient(name = "order-service")
public interface OrderServiceFeign {
    
    /**
     * 创建订单
     * @param orderId 订单ID
     * @param goodsId 商品ID
     * @param num 数量
     * @param userId 用户ID
     * @param amount 金额
     * @return 创建结果
     */
    @PostMapping("/order/create")
    String createOrder(
            @RequestParam("orderId") String orderId,
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("userId") String userId,
            @RequestParam("amount") long amount
    );
}