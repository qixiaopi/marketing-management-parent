package com.qixiaopi.point.util;

import com.qixiaopi.point.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class LockUtil {

    @Autowired
    private DistributedLock distributedLock;

    /**
     * 动态计算锁过期时间
     * 根据积分数量动态调整锁过期时间，确保足够的处理时间
     */
    public static long calculateLockTimeout(Long amount) {
        // 基础时间 5 秒
        long baseTimeout = 5;
        // 每增加 1000 积分，增加 1 秒处理时间
        long extraTimeout = amount / 1000;
        // 最大不超过 30 秒
        return Math.min(baseTimeout + extraTimeout, 30);
    }

    /**
     * 执行带分布式锁的操作
     */
    public <T> T executeWithLock(String lockKey, Long amount, Supplier<T> supplier) {
        long lockTimeout = calculateLockTimeout(amount);
        boolean locked = false;
        try {
            locked = distributedLock.tryLock(lockKey, lockTimeout, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败 - 锁键: {}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            return supplier.get();
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    /**
     * 执行带分布式锁的操作，返回 ResultDTO
     */
    public ResultDTO<String> executeWithLockForResult(String lockKey, Long amount, Supplier<ResultDTO<String>> supplier) {
        try {
            return executeWithLock(lockKey, amount, supplier);
        } catch (Exception e) {
            log.error("执行带锁操作失败 - 锁键: {}, 错误: {}", lockKey, e.getMessage());
            return ResultDTO.failure(e.getMessage());
        }
    }
}