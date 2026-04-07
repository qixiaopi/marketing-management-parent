package com.qixiaopi.seckill.mq;

import com.qixiaopi.seckill.dto.SeckillOrderMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消息生产者
 * 异步下单，削峰填谷
 */
@Slf4j
@Component
public class SeckillOrderProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";
    @Autowired
    private RedissonClient redissonClient;
    /**
     * 发送秒杀订单消息
     */
    public void sendSeckillOrderMessage(SeckillOrderMessage message) {
        int maxRetries = 1; // 减少重试次数，避免消息重复发送
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                rocketMQTemplate.asyncSend(SECKILL_ORDER_TOPIC, 
                    MessageBuilder.withPayload(message).build(), 
                    new org.apache.rocketmq.client.producer.SendCallback() {
                        @Override
                        public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                            log.info("秒杀订单消息发送成功，订单号：{}，消息ID：{}", 
                                message.getOrderId(), sendResult.getMsgId());
                        }
                        @Override
                        public void onException(Throwable e) {
                            log.error("秒杀订单消息发送失败，订单号：{}", message.getOrderId(), e);
                            // 记录到死信队列，需要人工确认事务状态
                            recordToDeadLetterQueue(message.getOrderId(), message.getGoodsId(), message.getNum(), "秒杀订单消息发送失败，需要人工确认事务状态");
                        }
                    });
                return; // 发送成功，直接返回
            } catch (Exception e) {
                retryCount++;
                log.error("发送秒杀订单消息异常，订单号：{}，重试次数：{}/{}", 
                    message.getOrderId(), retryCount, maxRetries, e);
                
                if (retryCount >= maxRetries) {
                    log.error("发送秒杀订单消息失败，已达到最大重试次数，订单号：{}", message.getOrderId());
                    throw new RuntimeException("秒杀下单失败，请重试");
                }
                
                // 指数退避策略
                try {
                    Thread.sleep(100 * (1 << retryCount));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("发送消息被中断", ie);
                }
            }
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
}
