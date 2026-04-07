package com.qixiaopi.inventory.service;

public interface InventoryService {
    Boolean deductStock(Long skuId, Integer num, String orderNo);
}
