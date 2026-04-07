package com.qixiaopi.seckill.utils;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 库存工具类
 * 封装库存相关的公共方法
 */
@Slf4j
@Component
public class StockUtils {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化分段库存
     */
    public void initSegmentStock(Long goodsId, Integer totalStock, Integer segmentCount) {
        // 使用哈希标签确保所有相关键都在同一个哈希槽中
        String stockKey = "seckill:stock:{" + goodsId + "}";
        
        int segmentSize = totalStock / segmentCount;
        int remainder = totalStock % segmentCount;
        
        for (int i = 0; i < segmentCount; i++) {
            int size = segmentSize;
            if (i < remainder) {
                size += 1;
            }
            
            String segmentKey = stockKey + ":segment:" + i;
            RAtomicLong segmentAtomic = redissonClient.getAtomicLong(segmentKey);
            segmentAtomic.set(size);
        }
        
        // 设置分段数量
        RBucket<String> segmentCountBucket = redissonClient.getBucket(stockKey + ":segment:count");
        segmentCountBucket.set(String.valueOf(segmentCount));
        
        log.info("分段库存初始化成功，商品ID：{}，总库存：{}，分段数：{}", goodsId, totalStock, segmentCount);
    }
    /**
     * 回滚Redis库存
     * 支持分段库存回滚
     */
    public void rollbackRedisStock(Long goodsId, Integer num) {
        try {
            // 使用哈希标签确保所有相关键都在同一个哈希槽中
            String stockKey = "seckill:stock:{" + goodsId + "}";
            RBucket<String> segmentCountBucket = redissonClient.getBucket(stockKey + ":segment:count");
            String segmentCountStr = segmentCountBucket.get();

            if (segmentCountStr != null) {
                // 使用分段库存，回滚到随机分段，避免集中到一个分段
                int segmentCount = Integer.parseInt(segmentCountStr);
                int segmentIndex = (int) (System.currentTimeMillis() % segmentCount);
                String segmentKey = stockKey + ":segment:" + segmentIndex;
                RAtomicLong segmentAtomic = redissonClient.getAtomicLong(segmentKey);
                segmentAtomic.addAndGet(num);
                log.info("分段库存回滚成功，商品ID：{}，分段：{}，数量：{}", goodsId, segmentIndex, num);
            } else {
                // 回滚总库存
                RAtomicLong stockAtomic = redissonClient.getAtomicLong(stockKey);
                stockAtomic.addAndGet(num);
                log.info("总库存回滚成功，商品ID：{}，数量：{}", goodsId, num);
            }
        } catch (Exception e) {
            log.error("库存回滚失败，商品ID：{}，数量：{}", goodsId, num, e);
        }
    }
}