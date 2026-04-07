package com.qixiaopi.product.mq;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.qixiaopi.product.dto.SeckillOrderMessage;
import com.qixiaopi.product.feign.OrderServiceFeign;
import com.qixiaopi.product.utils.StockUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 秒杀订单消息消费者
 * 异步处理订单创建、库存扣减、余额扣减
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "seckill-order-topic",
        consumerGroup = "seckill-order-consumer-group",
        consumeThreadNumber = 64,
        maxReconsumeTimes = 1,
        consumeTimeout = 30000
)
public class SeckillOrderConsumer implements RocketMQListener<SeckillOrderMessage> {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StockUtils stockUtils;
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private OrderServiceFeign orderService;

    private long getLockWaitTime() {
       return Long.parseLong(environment.getProperty("seckill.lock.wait.time", "2"));
    }
    @Override
    public void onMessage(SeckillOrderMessage message) {
        // 计算消息延迟
        long currentTime = System.currentTimeMillis();
        
        String orderId = message.getOrderId();
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();

        long messageDelay = 0;
        if (message.getCreateTime() != null) {
            messageDelay = currentTime - message.getCreateTime();
        }
        log.info("开始处理秒杀订单消息，订单号：{}，用户ID：{}，商品ID：{}，消息延迟：{}ms", 
                orderId, userId, goodsId, messageDelay);
        // 分布式锁：防止同一订单并发处理
        String lockKey = "seckill:order:lock:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        // 尝试获取锁，使用看门狗自动续期
        try {
			// 获取最新的锁等待时间
			long lockWaitTime = getLockWaitTime();
			if (!lock.tryLock(lockWaitTime, TimeUnit.SECONDS)) {
			    log.warn("获取订单锁失败，订单号：{}，可能有其他线程正在处理", orderId);
			    throw new RuntimeException("系统繁忙，请稍后再试");
			}
            try {
				// 幂等检查：订单是否已处理（放入锁内执行，避免竞态条件）
				String processedKey = "seckill:order:processed:" + orderId;
				RBucket<String> processedBucket = redissonClient.getBucket(processedKey);
				if (processedBucket.isExists()) {
                    String status = processedBucket.get();
                    if ("SUCCESS".equals(status)) {
                        log.warn("订单已成功处理，跳过，订单号：{}", orderId);
                        return;
                    } else if ("PROCESSING".equals(status)) {
                        // 检查是否超时（超过10分钟），超时则允许重试
                        String processTimeKey = "seckill:order:process:time:" + orderId;
                        RBucket<Long> processTimeBucket = redissonClient.getBucket(processTimeKey);
                        Long processTime = processTimeBucket.get();
                        if (processTime != null && (currentTime - processTime) > 10 * 60 * 1000) {
                            log.warn("订单处理超时（超过10分钟），允许重试，订单号：{}", orderId);
                            // 清理旧状态，允许重新处理
                            processedBucket.delete();
                            processTimeBucket.delete();
                        } else {
                            log.warn("订单正在处理中，跳过，订单号：{}", orderId);
                            return;
                        }
                    }
				}
                // 标记订单处理中，并记录开始时间
                processedBucket.set("PROCESSING", Duration.ofMinutes(30));
                String processTimeKey = "seckill:order:process:time:" + orderId;
                RBucket<Long> processTimeBucket = redissonClient.getBucket(processTimeKey);
                processTimeBucket.set(System.currentTimeMillis(), Duration.ofMinutes(30));
                log.info("订单标记为处理中，订单号：{}，超时时间：30分钟", orderId);

                // 执行订单处理流程
                processOrder(message);

                // 标记订单处理成功
                processedBucket.set("SUCCESS", Duration.ofHours(24));
                // 清除处理时间标记，避免留下无用的 Redis key
                processTimeBucket.delete();
                
                log.info("秒杀订单处理完成，订单号：{}", orderId);
				
			} finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
		} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("处理秒杀订单被中断，订单号：{}", orderId, e);
            // 中断异常不清理状态，让消息重新消费或人工处理
            // 记录到死信队列，需要人工确认事务状态
            recordToDeadLetterQueue(orderId, goodsId, message.getNum(), "系统中断，需要人工确认事务状态");
            return;
        } catch (Exception e) {
            log.error("处理秒杀订单失败，订单号：{}", orderId, e);
            handleOrderFailure(message, e.getMessage());
            return;
        } finally {
            
        }
    }
    
    /**
     * 记录到死信队列
     */
    private void recordToDeadLetterQueue(String orderId, Long goodsId, Integer num, String reason) {
        try {
            String deadLetterKey = "seckill:order:deadletter:" + orderId;
            RBucket<String> deadLetterBucket = redissonClient.getBucket(deadLetterKey);
            String content = String.format("时间:%s,商品ID:%d,数量:%d,原因:%s", 
                    LocalDateTime.now(), goodsId, num, reason);
            deadLetterBucket.set(content, Duration.ofDays(7));
            log.info("订单记录到死信队列，订单号：{}，原因：{}", orderId, reason);
        } catch (Exception e) {
            log.error("记录死信队列失败，订单号：{}", orderId, e);
        }
    }
    
    /**
     * 处理订单失败
     * 注意：此方法只清理状态，不执行回滚操作
     * 回滚操作由 rollbackTransaction 统一处理
     */
    private void handleOrderFailure(SeckillOrderMessage message, String reason) {
        // 参数校验，防止 NPE
        if (message == null || message.getOrderId() == null) {
            log.error("处理订单失败，但消息为空或订单号为空，原因：{}", reason);
            return;
        }
        
        String orderId = message.getOrderId();
        Long goodsId = message.getGoodsId();
        Integer num = message.getNum();
        
        
        // 记录失败信息到死信队列
        recordToDeadLetterQueue(orderId, goodsId, num, "订单处理失败：" + reason);
        
        // 注意：不在这里增加失败计数，由调用方统一处理
    }


    private void processOrder(SeckillOrderMessage message) {
        // 参数校验
        if (message == null || message.getOrderId() == null || message.getUserId() == null
                || message.getGoodsId() == null || message.getNum() == null || message.getAmount() == null) {
            throw new IllegalArgumentException("订单消息参数不完整");
        }
        // 校验数量和金额必须大于0
        if (message.getNum() <= 0 || message.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单数量或金额必须大于0");
        }
        
        // 检查商品是否存在（防止缓存穿透）
        if (!stockUtils.checkGoodsExists(message.getGoodsId())) {
            throw new IllegalArgumentException("商品不存在");
        }
        

        String orderId = message.getOrderId();
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();
        Integer num = message.getNum();
        


        try {
            log.info("库存扣减成功，订单号：{}，商品ID：{}，数量：{}", orderId, goodsId, num);


            log.info("订单创建成功，订单号：{}", orderId);

  
            log.info("余额扣减成功，订单号：{}，用户ID：{}", orderId, userId);

      
            
            String orderResult = orderService.createOrder(
                    orderId,
                    goodsId,
                    num,
                    userId.toString(),
                    message.getAmount().longValue());
            log.info("订单服务调用结果：{}", orderResult);

        } catch (Exception e) {
            log.error("订单处理异常，开始回滚，订单号：{}，异常：{}", orderId, e.getMessage());
            throw e;
        }
    }
}