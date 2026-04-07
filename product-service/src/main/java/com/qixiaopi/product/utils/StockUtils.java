package com.qixiaopi.product.utils;

import java.time.Duration;

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
     * 检查商品是否存在（防止缓存穿透）
     * @param goodsId 商品ID
     * @return 是否存在
     */
    public boolean checkGoodsExists(Long goodsId) {
        try {
            String existsKey = "seckill:goods:exists:" + goodsId;
            RBucket<Boolean> existsBucket = redissonClient.getBucket(existsKey);
            
            // 先检查缓存
            if (existsBucket.isExists()) {
                return existsBucket.get();
            }
            
            // 这里应该从数据库查询商品是否存在
            // 暂时返回true，实际项目中需要实现数据库查询
            boolean exists = true;
            
            // 缓存结果，设置较短的过期时间
            existsBucket.set(exists, Duration.ofMinutes(30));
            return exists;
        } catch (Exception e) {
            log.error("检查商品存在性失败，商品ID：{}", goodsId, e);
            // 异常时返回true，避免缓存穿透
            return true;
        }
    }
}