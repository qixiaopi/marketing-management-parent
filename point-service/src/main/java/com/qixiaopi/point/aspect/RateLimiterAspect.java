package com.qixiaopi.point.aspect;

import com.qixiaopi.point.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private static final ConcurrentHashMap<String, AtomicInteger> minuteCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> secondCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> minuteTimestamps = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> secondTimestamps = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        String key = rateLimiter.key();
        int permitsPerMinute = rateLimiter.permitsPerMinute();
        int permitsPerSecond = rateLimiter.permitsPerSecond();

        if (permitsPerMinute > 0 && !checkMinuteLimit(key, permitsPerMinute)) {
            log.warn("限流拦截 - key: {}, 超过每分钟限制: {}", key, permitsPerMinute);
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        if (permitsPerSecond > 0 && !checkSecondLimit(key, permitsPerSecond)) {
            log.warn("限流拦截 - key: {}, 超过每秒限制: {}", key, permitsPerSecond);
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private boolean checkMinuteLimit(String key, int permitsPerMinute) {
        long currentTime = System.currentTimeMillis();
        AtomicInteger counter = minuteCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTimestamp = minuteTimestamps.get(key);

        if (lastTimestamp == null || currentTime - lastTimestamp >= 60000) {
            counter.set(0);
            minuteTimestamps.put(key, currentTime);
        }

        return counter.incrementAndGet() <= permitsPerMinute;
    }

    private boolean checkSecondLimit(String key, int permitsPerSecond) {
        long currentTime = System.currentTimeMillis();
        AtomicInteger counter = secondCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTimestamp = secondTimestamps.get(key);

        if (lastTimestamp == null || currentTime - lastTimestamp >= 1000) {
            counter.set(0);
            secondTimestamps.put(key, currentTime);
        }

        return counter.incrementAndGet() <= permitsPerSecond;
    }
}
