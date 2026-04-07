package com.qixiaopi.seckill.service.impl;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qixiaopi.seckill.dto.SeckillOrderMessage;
import com.qixiaopi.seckill.mq.SeckillOrderProducer;
import com.qixiaopi.seckill.service.SeckillService;
import com.qixiaopi.seckill.utils.StockUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 秒杀服务实现
 * 1. Redis预减库存，减少数据库压力
 * 2. 异步MQ下单，削峰填谷
 * 3. 令牌桶限流
 * 4. 库存分段减少锁竞争
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StockUtils stockUtils;
    @Autowired
    private SeckillOrderProducer seckillOrderProducer;
    /**
     * 优化的秒杀方法
     * 1. 参数验证
     * 2. 令牌桶限流
     * 3. 用户购买限制
     * 4. Redis预减库存（Lua原子操作）
     * 5. 异步MQ下单
     */
    @Override
    public String doSeckill(Long userId, Long goodsId, Integer num) {
    	long startTime = System.currentTimeMillis();
        // 1. 参数验证
        if (num == null || num <= 0) {
            return "购买数量无效";
        }
    	
        // 4. Redis预减库存（使用分段库存减少锁竞争）
        int stockResult = deductStockWithSegment(goodsId, num);
        if (stockResult == -1) {
            // 库存未初始化，尝试从数据库加载
            if (!initStockFromDb(goodsId)) {
                return "商品不存在或库存未初始化";
            }
            // 重试扣减
            stockResult = deductStockWithSegment(goodsId, num);
        }

        if (stockResult == 0) {
        	log.warn("库存不足，商品ID：{}，请求数量：{}", goodsId, num);
            return "商品库存不足";
        }

        if (stockResult == -2) {
        	log.warn("库存不足（部分），商品ID：{}，请求数量：{}", goodsId, num);
            return "商品库存不足";
        }

        if (stockResult == -1) {
        	log.warn("库存初始化失败或扣减异常，商品ID：{}", goodsId);
            return "系统繁忙，请稍后再试";
        }
        String orderId = generateOrderId();
        BigDecimal price = BigDecimal.ONE;
           // 8. 发送异步订单消息
           SeckillOrderMessage message = SeckillOrderMessage.builder()
                   .orderId(orderId)
                   .userId(userId)
                   .goodsId(goodsId)
                   .num(num)
                   .amount(price.multiply(BigDecimal.valueOf(num)))
                   .createTime(System.currentTimeMillis())
                   .build();
          seckillOrderProducer.sendSeckillOrderMessage(message);
       
       long costTime = System.currentTimeMillis() - startTime;
       log.info("秒杀请求处理成功，订单号：{}，耗时：{}ms", orderId, costTime);
    	return null;
    }
    /**
     * 生成订单ID
     */
    private String generateOrderId() {
        return "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }
    /**
     * 使用分段库存扣减
     * 减少锁竞争，提高并发性能
     */
    private int deductStockWithSegment(Long goodsId, Integer num) {
        // 使用哈希标签确保所有相关键都在同一个哈希槽中
        String stockKey = "seckill:stock:{" + goodsId + "}";
        RBucket<String> segmentCountBucket = redissonClient.getBucket(stockKey + ":segment:count");
        String segmentCountStr = segmentCountBucket.get();

        if (segmentCountStr == null) {
            // 未使用分段库存，使用普通扣减
            return deductStockAtomic(stockKey, num);
        }

        int segmentCount = Integer.parseInt(segmentCountStr);
        // 随机选择分段，减少竞争
        int segmentIndex = (int) (System.currentTimeMillis() % segmentCount);

        for (int i = 0; i < segmentCount; i++) {
            int trySegment = (segmentIndex + i) % segmentCount;
            String segmentKey = stockKey + ":segment:" + trySegment;
            int result = deductStockAtomic(segmentKey, num);
            if (result == 1) {
                return 1; // 扣减成功
            }
        }

        return 0; // 所有分段库存不足
    }

    /**
     * 原子性扣减库存（使用Redisson的RAtomicLong）
     */
    private int deductStockAtomic(String stockKey, Integer num) {
        try {
            RAtomicLong stockAtomic = redissonClient.getAtomicLong(stockKey);
            
            // 检查库存是否存在
            if (!stockAtomic.isExists()) {
                return -1;
            }
            
            long currentStock = stockAtomic.get();
            if (currentStock <= 0) {
                return 0;
            }
            
            if (currentStock < num) {
                return -2;
            }
            
            // 原子性扣减
            long remaining = stockAtomic.addAndGet(-num);
            if (remaining < 0) {
                // 如果扣减后为负数，回滚
                stockAtomic.addAndGet(num);
                return 0;
            }
            
            return 1;
        } catch (Exception e) {
            log.error("扣减库存异常，key：{}，num：{}", stockKey, num, e);
            return -1;
        }
    }

    /**
     * 从数据库初始化库存
     * 使用分布式锁 + 双重检查，确保高并发下只初始化一次
     */
    private boolean initStockFromDb(Long goodsId) {
        // 使用哈希标签确保所有相关键都在同一个哈希槽中
        String stockKey = "seckill:stock:{" + goodsId + "}";
        String segmentCountKey = stockKey + ":segment:count";

        // 第一重检查：快速判断是否已经初始化
        RBucket<String> segmentCountBucket = redissonClient.getBucket(segmentCountKey);
        if (segmentCountBucket.isExists()) {
            return true;
        }

        String lockKey = "seckill:stock:init:lock:{" + goodsId + "}";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待3秒，锁持有时间5秒
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                log.warn("获取库存初始化锁失败，商品ID：{}，可能有其他线程正在初始化", goodsId);
                // 等待后再次检查
                Thread.sleep(50);
                return segmentCountBucket.isExists();
            }

            try {
                // 第二重检查：获取锁后再次确认
                if (segmentCountBucket.isExists()) {
                    return true;
                }

                // 从数据库获取库存（在锁外获取，减少锁持有时间）
				/*
				 * ResultDTO<Integer> stockResult = productFeignClient.getStock(goodsId); if
				 * (!stockResult.isSuccess() || stockResult.getData() == null) {
				 * log.error("从数据库获取库存失败，商品ID：{}，结果：{}", goodsId, stockResult); return false; }
				 * 
				 * Integer stock = stockResult.getData();
				 */
                Integer stock=10;
                if (stock <= 0) {
                    log.warn("商品库存为0或负数，商品ID：{}，库存：{}", goodsId, stock);
                    return false;
                }

                // 使用分段库存（默认10个分段）
                initSegmentStock(goodsId, stock, 10);

                log.info("库存初始化成功，商品ID：{}，库存：{}，分段：10", goodsId, stock);
                return true;

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("库存初始化被中断，商品ID：{}", goodsId, e);
            return false;
        } catch (Exception e) {
            log.error("库存初始化异常，商品ID：{}", goodsId, e);
            return false;
        }
    }

    /**
     * 初始化分段库存
     */
    private void initSegmentStock(Long goodsId, Integer totalStock, Integer segmentCount) {
        stockUtils.initSegmentStock(goodsId, totalStock, segmentCount);
    }
}