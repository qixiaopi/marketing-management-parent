package com.qixiaopi.order.service;

import com.qixiaopi.order.entity.OrderCreateMessage;

public interface OrderService {
	/**
     * 创建订单
     * @param orderMessage 订单创建消息
     * @return 创建结果
     */
	String createOrder(OrderCreateMessage orderMessage);
}
