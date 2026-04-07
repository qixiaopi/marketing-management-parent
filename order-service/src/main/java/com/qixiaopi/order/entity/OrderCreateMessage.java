package com.qixiaopi.order.entity;

import lombok.Data;

@Data
public class OrderCreateMessage {
    private String orderId;
    private String userId;
    private Long skuId;
    private Integer num;
    private Long orderAmount;
}
