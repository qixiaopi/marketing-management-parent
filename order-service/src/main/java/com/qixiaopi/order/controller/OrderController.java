package com.qixiaopi.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qixiaopi.order.entity.OrderCreateMessage;
import com.qixiaopi.order.service.OrderService;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public String createOrder(String orderId, Long goodsId, Integer num, String userId, long amount) {
    	OrderCreateMessage orderMessage = new OrderCreateMessage();
        orderMessage.setOrderId(orderId);
        orderMessage.setSkuId(goodsId);
        orderMessage.setNum(num);
        orderMessage.setUserId(userId);
        orderMessage.setOrderAmount(amount);
        return orderService.createOrder(orderMessage);
    }
}
