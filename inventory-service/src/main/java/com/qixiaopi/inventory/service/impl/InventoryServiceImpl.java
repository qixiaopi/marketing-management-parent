package com.qixiaopi.inventory.service.impl;

import com.qixiaopi.inventory.entity.Stock;
import com.qixiaopi.inventory.entity.InventoryLog;
import com.qixiaopi.inventory.mapper.InventoryLogMapper;
import com.qixiaopi.inventory.mapper.StockMapper;
import com.qixiaopi.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deductStock(Long skuId, Integer num, String orderNo) {
        log.info("开始扣减库存 - SKU: {}, 扣减数量: {}, 订单号: {}", skuId, num, orderNo);
        
        try {
            // 使用行锁查询库存，确保并发安全
            Stock inventory = stockMapper.selectByIdForUpdate(skuId);
            if (inventory == null) {
                log.warn("库存记录不存在 - SKU: {}, 订单号: {}", skuId, orderNo);
                return false;
            }
            
            log.info("查询到当前库存(加锁) - SKU: {}, 可用库存: {}, 订单号: {}", 
                     skuId, inventory.getAvailableStock(), orderNo);
            
            if (inventory.getAvailableStock() < num) {
                log.warn("库存不足 - SKU: {}, 可用库存: {}, 扣减数量: {}, 订单号: {}", 
                         skuId, inventory.getAvailableStock(), num, orderNo);
                return false;
            }

            int beforeStock = inventory.getAvailableStock();
            int afterStock = beforeStock - num;

            // 直接更新库存并记录流水（在同一事务中）
            int rows = stockMapper.deductStock(skuId, num);
            if (rows == 0) {
                log.warn("库存扣减失败 - SKU: {}, 扣减数量: {}, 订单号: {}", 
                         skuId, num, orderNo);
                return false;
            }
            
            // 记录库存流水（使用行锁获取的准确beforeStock）
            InventoryLog inventoryLog = new InventoryLog();
            inventoryLog.setSkuId(skuId);
            inventoryLog.setOrderNo(orderNo);
            inventoryLog.setChangeQuantity(-num);
            inventoryLog.setBeforeStock(beforeStock);
            inventoryLog.setAfterStock(afterStock);
            inventoryLog.setCreateTime(LocalDateTime.now());
            inventoryLogMapper.insert(inventoryLog);
            
            log.info("库存扣减成功 - SKU: {}, 扣减前库存: {}, 扣减后库存: {}, 扣减数量: {}, 订单号: {}", 
                     skuId, beforeStock, afterStock, num, orderNo);
            
            return true;
            
        } catch (Exception e) {
            log.error("库存扣减异常 - SKU: {}, 扣减数量: {}, 订单号: {}, 错误信息: {}", 
                      skuId, num, orderNo, e.getMessage(), e);
            throw e;
        }
    }
}
