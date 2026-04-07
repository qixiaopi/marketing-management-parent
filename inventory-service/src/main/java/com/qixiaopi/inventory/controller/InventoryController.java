package com.qixiaopi.inventory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qixiaopi.inventory.dto.ResultDTO;
import com.qixiaopi.inventory.service.InventoryService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/deductStock")
    public ResultDTO<Boolean> deductStock(@RequestParam Long skuId, @RequestParam Integer num, @RequestParam String orderNo) {
        Boolean result = inventoryService.deductStock(skuId, num, orderNo);
        if (result) {
            return ResultDTO.success(result);
        } else {
            return ResultDTO.failure("库存不足");
        }
    }
}
