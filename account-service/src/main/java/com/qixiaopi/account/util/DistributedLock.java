package com.qixiaopi.account.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DistributedLock {

    @Autowired
    private RedissonClient redissonClient;

    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            // 尝试获取锁，不等待，锁自动过期时间为 timeout
            // Redisson 会自动启动看门狗机制续期
            return lock.tryLock(0, timeout, unit);
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败 - key: {}, error: {}", key, e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁失败 - key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }

    public void unlock(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("释放分布式锁失败 - key: {}, error: {}", key, e.getMessage());
        }
    }

    public boolean isLocked(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.isLocked();
        } catch (Exception e) {
            log.error("检查锁状态失败 - key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }
}
