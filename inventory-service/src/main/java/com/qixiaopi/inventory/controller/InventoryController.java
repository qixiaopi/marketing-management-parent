package com.qixiaopi.inventory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.qixiaopi.inventory.dto.ResultDTO;
import com.qixiaopi.inventory.service.InventoryService;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/deductStock")
    public ResultDTO<Boolean> deductStock(@RequestParam Long skuId, @RequestParam Integer num, @RequestParam String orderNo) {
        // 从请求头中获取XID
        String xid = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            xid = attributes.getRequest().getHeader(RootContext.KEY_XID);
            log.info("从请求头获取到XID: {}", xid);
            if (xid != null) {
                RootContext.bind(xid);
                log.info("绑定XID到RootContext: {}", xid);
            }
        }
        
        // 打印当前XID，用于调试
        String currentXid = RootContext.getXID();
        log.info("库存服务当前XID: {}", currentXid);
        
        Boolean result = inventoryService.deductStock(skuId, num, orderNo);
        if (result) {
            return ResultDTO.success(result);
        } else {
            return ResultDTO.failure("库存不足");
        }
    }
}
