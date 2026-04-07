package com.qixiaopi.account.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.qixiaopi.account.dto.ResultDTO;
import com.qixiaopi.account.entity.Account;
import com.qixiaopi.account.entity.AccountLog;
import com.qixiaopi.account.mapper.AccountMapper;
import com.qixiaopi.account.mapper.AccountLogMapper;
import com.qixiaopi.account.service.AccountService;
import com.qixiaopi.account.service.DuplicateRequestLogService;
import com.qixiaopi.account.util.DistributedLock;
import com.qixiaopi.account.util.MoneyUtil;
import com.qixiaopi.account.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountLogMapper accountLogMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private DuplicateRequestLogService duplicateRequestLogService;

    private AccountServiceImpl getProxy() {
        return applicationContext.getBean(AccountServiceImpl.class);
    }

    @Override
    public ResultDTO<String> deductAccount(String userId, Long amount, String orderNo) {
        log.info("开始扣减账户余额 - 用户ID: {}, 扣减金额: {}分({}元), 订单号: {}",
                userId, amount, MoneyUtil.fenToYuanStr(amount), orderNo);

        // 参数校验
        String validateResult = validateParams(userId, amount, orderNo);
        if (validateResult != null) {
            log.warn("参数校验失败 - {}", validateResult);
            return ResultDTO.failure(validateResult);
        }

        // 使用分布式锁保证幂等性
        String lockKey = "account:order:" + orderNo;
        boolean locked = false;
        try {
            // 尝试获取锁，锁自动过期时间10秒
            locked = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败 - 订单号: {}", orderNo);
                // 记录重复请求
                duplicateRequestLogService.logDuplicateRequest(userId, orderNo, "ACCOUNT_DEDUCT", "LOCK_FAILED");
                return ResultDTO.failure("系统繁忙，请稍后重试");
            }

            try {
                // 根据user_id选择数据源：user_id % 2
                Long userIdLong = Long.valueOf(userId);
                ResultDTO<String> result;
                if (userIdLong % 2 == 0) {
                    result = getProxy().deductAccountInMaster0(userId, amount, orderNo);
                } else {
                    result = getProxy().deductAccountInMaster1(userId, amount, orderNo);
                }

                return result;
            } catch (DuplicateKeyException e) {
                // 唯一索引冲突，记录重复请求
                log.error("唯一索引冲突 - 用户ID: {}, 订单号: {}, 错误: {}", userId, orderNo, e.getMessage());
                duplicateRequestLogService.logDuplicateRequest(userId, orderNo, "ACCOUNT_DEDUCT", "DUPLICATE_KEY");
                return ResultDTO.success("扣减成功", "该订单已处理过");
            } catch (Exception e) {
                log.error("账户扣减异常 - 用户ID: {}, 扣减金额: {}, 订单号: {}, 错误信息: {}",
                        userId, amount, orderNo, e.getMessage(), e);
                return ResultDTO.failure("账户扣减失败：" + e.getMessage());
            }
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    /**
     * 参数校验
     */
    private String validateParams(String userId, Long amount, String orderNo) {
        String errorMsg = ValidationUtil.validateUserId(userId);
        if (errorMsg != null) return errorMsg;

        errorMsg = ValidationUtil.validateAmount(amount);
        if (errorMsg != null) return errorMsg;

        errorMsg = ValidationUtil.validateOrderNo(orderNo);
        if (errorMsg != null) return errorMsg;

        return null;
    }

    @DS("master0")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<String> deductAccountInMaster0(String userId, Long amount, String orderNo) {
        return doDeductAccount(userId, amount, orderNo, "master0");
    }

    @DS("master1")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<String> deductAccountInMaster1(String userId, Long amount, String orderNo) {
        return doDeductAccount(userId, amount, orderNo, "master1");
    }

    private ResultDTO<String> doDeductAccount(String userId, Long amount, String orderNo, String dataSource) {
        log.info("开始扣减账户余额 - 用户ID: {}, 扣减金额: {}分({}元), 订单号: {}, 数据源: {}",
                userId, amount, MoneyUtil.fenToYuanStr(amount), orderNo, dataSource);

        try {
            // 检查订单是否已经处理过（幂等性预检查）
            if (isOrderProcessed(orderNo)) {
                log.warn("订单已处理过 - 用户ID: {}, 订单号: {}, 数据源: {}", userId, orderNo, dataSource);
                return ResultDTO.success("扣减成功", "该订单已处理过");
            }

            // 使用行锁查询账户，确保并发安全
            Account account = accountMapper.selectByUserIdForUpdate(userId);
            if (account == null) {
                log.warn("账户不存在 - 用户ID: {}, 订单号: {}, 数据源: {}", userId, orderNo, dataSource);
                return ResultDTO.failure("账户不存在");
            }

            log.info("查询到当前账户余额(加锁) - 用户ID: {}, 余额: {}分({}元), 订单号: {}, 数据源: {}",
                    userId, account.getBalance(), MoneyUtil.fenToYuanStr(account.getBalance()),
                    orderNo, dataSource);

            if (account.getBalance() < amount) {
                log.warn("余额不足 - 用户ID: {}, 当前余额: {}分({}元), 扣减金额: {}分({}元), 订单号: {}, 数据源: {}",
                        userId, account.getBalance(), MoneyUtil.fenToYuanStr(account.getBalance()),
                        amount, MoneyUtil.fenToYuanStr(amount), orderNo, dataSource);
                return ResultDTO.failure("余额不足");
            }

            long beforeBalance = account.getBalance();
            long afterBalance = beforeBalance - amount;

            // 扣减账户余额
            int rows = accountMapper.deduct(userId, amount);
            if (rows == 0) {
                log.warn("账户扣减失败 - 用户ID: {}, 扣减金额: {}, 订单号: {}, 数据源: {}",
                        userId, amount, orderNo, dataSource);
                return ResultDTO.failure("账户扣减失败");
            }

            // 记录账户流水（使用行锁获取的准确beforeBalance）
            AccountLog accountLog = new AccountLog();
            accountLog.setUserId(userId);
            accountLog.setOrderNo(orderNo);
            accountLog.setChangeAmount(-amount);
            accountLog.setBeforeBalance(beforeBalance);
            accountLog.setAfterBalance(afterBalance);
            accountLog.setType(2); // 2-扣减
            accountLog.setRemark("订单扣减");
            accountLog.setCreateTime(LocalDateTime.now());
            accountLogMapper.insert(accountLog);

            log.info("账户扣减成功 - 用户ID: {}, 扣减前余额: {}分({}元), 扣减后余额: {}分({}元), " +
                            "扣减金额: {}分({}元), 订单号: {}, 数据源: {}",
                    userId, beforeBalance, MoneyUtil.fenToYuanStr(beforeBalance),
                    afterBalance, MoneyUtil.fenToYuanStr(afterBalance),
                    amount, MoneyUtil.fenToYuanStr(amount), orderNo, dataSource);

            return ResultDTO.success("扣减成功");

        } catch (Exception e) {
            log.error("账户扣减异常 - 用户ID: {}, 扣减金额: {}, 订单号: {}, 数据源: {}, 错误信息: {}",
                    userId, amount, orderNo, dataSource, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 检查订单是否已经处理过（幂等性预检查）
     */
    private boolean isOrderProcessed(String orderNo) {
        try {
            // 使用自定义的 selectIdByOrderNo 方法，只查询id，性能更好
            Long existId = accountLogMapper.selectIdByOrderNo(orderNo);
            return existId != null;
        } catch (Exception e) {
            log.error("检查订单是否已处理失败 - 订单号: {}, 错误: {}", orderNo, e.getMessage());
            return false;
        }
    }
}
