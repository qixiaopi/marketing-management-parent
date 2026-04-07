package com.qixiaopi.account.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.qixiaopi.account.mapper.DuplicateRequestLogMapper;
import com.qixiaopi.account.service.DuplicateRequestLogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DuplicateRequestLogServiceImpl implements DuplicateRequestLogService {

    @Autowired
    private DuplicateRequestLogMapper duplicateRequestLogMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private DuplicateRequestLogServiceImpl getProxy() {
        return applicationContext.getBean(DuplicateRequestLogServiceImpl.class);
    }

    @Override
    public void logDuplicateRequest(String userId, String orderNo, String requestType, String rejectReason) {
        try {
            Long userIdLong = Long.valueOf(userId);
            if (userIdLong % 2 == 0) {
                getProxy().logDuplicateRequestInMaster0(userId, orderNo, requestType, rejectReason);
            } else {
                getProxy().logDuplicateRequestInMaster1(userId, orderNo, requestType, rejectReason);
            }
        } catch (Exception e) {
            log.error("记录重复请求失败 - 用户ID: {}, 订单号: {}, 错误: {}", userId, orderNo, e.getMessage());
        }
    }

    @DS("master0")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logDuplicateRequestInMaster0(String userId, String orderNo, String requestType, String rejectReason) {
        try {
            duplicateRequestLogMapper.insertLog(userId, orderNo, requestType, rejectReason, LocalDateTime.now());
            log.warn("记录重复请求(master0) - 用户ID: {}, 订单号: {}, 类型: {}, 原因: {}",
                    userId, orderNo, requestType, rejectReason);
        } catch (Exception e) {
            log.error("记录重复请求失败(master0) - 用户ID: {}, 订单号: {}, 错误: {}", userId, orderNo, e.getMessage());
        }
    }

    @DS("master1")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logDuplicateRequestInMaster1(String userId, String orderNo, String requestType, String rejectReason) {
        try {
            duplicateRequestLogMapper.insertLog(userId, orderNo, requestType, rejectReason, LocalDateTime.now());
            log.warn("记录重复请求(master1) - 用户ID: {}, 订单号: {}, 类型: {}, 原因: {}",
                    userId, orderNo, requestType, rejectReason);
        } catch (Exception e) {
            log.error("记录重复请求失败(master1) - 用户ID: {}, 订单号: {}, 错误: {}", userId, orderNo, e.getMessage());
        }
    }
}
