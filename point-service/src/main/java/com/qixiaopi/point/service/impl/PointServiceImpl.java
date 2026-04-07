package com.qixiaopi.point.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.qixiaopi.point.annotation.RateLimiter;
import com.qixiaopi.point.dto.ResultDTO;
import com.qixiaopi.point.entity.Point;
import com.qixiaopi.point.entity.PointLog;
import com.qixiaopi.point.exception.BusinessException;
import com.qixiaopi.point.mapper.PointLogMapper;
import com.qixiaopi.point.mapper.PointMapper;
import com.qixiaopi.point.service.DuplicateRequestLogService;
import com.qixiaopi.point.service.PointService;
import com.qixiaopi.point.util.LockUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PointServiceImpl implements PointService {

    @Autowired
    private PointMapper pointMapper;

    @Autowired
    private PointLogMapper pointLogMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DuplicateRequestLogService duplicateRequestLogService;

    @Autowired
    private LockUtil lockUtil;

    private PointServiceImpl getProxy() {
        return applicationContext.getBean(PointServiceImpl.class);
    }
    
    @RateLimiter(key = "addPoint", permitsPerMinute = 6000)
    @Override
    public ResultDTO<String> addPoint(Long userId, Long amount, String orderNo, Integer expireDays) {
        log.info("开始发放积分 - 用户ID: {}, 积分数量: {}, 订单号: {}, 过期天数: {}", userId, amount, orderNo, expireDays);

        // 参数校验
        validateParams(userId, amount, orderNo);
        
        // 校验过期天数
        if (expireDays != null && expireDays <= 0) {
            throw new BusinessException("PARAM_ERROR", "过期天数必须大于0");
        }
        if (expireDays != null && expireDays > 3650) { // 假设最大过期天数为10年
            throw new BusinessException("PARAM_ERROR", "过期天数不能超过3650天");
        }

        // 使用分布式锁保证幂等性（同时对用户ID和订单号加锁，避免并发插入冲突）
        String lockKey = "point:user:" + userId + ":order:" + orderNo;
        log.info("准备获取分布式锁 - 锁键: {}, 积分数量: {}", lockKey, amount);

        return lockUtil.executeWithLockForResult(lockKey, amount, () -> {
            int retryCount = 3;
            while (retryCount > 0) {
                try {
                    // 根据user_id选择数据源：user_id % 2
                    ResultDTO<String> result;
                    if (userId % 2 == 0) {
                        log.info("选择数据源 - 用户ID: {}, 数据源: master0", userId);
                        result = getProxy().addPointInMaster0(userId, amount, orderNo, expireDays);
                    } else {
                        log.info("选择数据源 - 用户ID: {}, 数据源: master1", userId);
                        result = getProxy().addPointInMaster1(userId, amount, orderNo, expireDays);
                    }

                    return result;
                } catch (DuplicateKeyException e) {
                    // 唯一索引冲突，记录重复请求
                    log.error("唯一索引冲突 - 用户ID: {}, 订单号: {}, 错误: {}", userId, orderNo, e.getMessage());
                    duplicateRequestLogService.logDuplicateRequest(String.valueOf(userId), orderNo, "POINT_ADD", "DUPLICATE_KEY");
                    return ResultDTO.success("积分发放成功", "该订单已处理过");
                } catch (org.springframework.dao.TransientDataAccessResourceException e) {
                    // 临时数据访问资源异常，包括死锁，进行重试
                    log.warn("数据库临时异常，可能是死锁，正在重试 - 剩余次数: {}", retryCount - 1);
                    retryCount--;
                    if (retryCount <= 0) {
                        throw e;
                    }
                    try {
                        Thread.sleep(100); // 短暂休眠后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } catch (Exception e) {
                    log.error("积分发放异常 - 用户ID: {}, 积分数量: {}, 订单号: {}, 错误信息: {}",
                            userId, amount, orderNo, e.getMessage(), e);
                    return ResultDTO.failure("积分发放失败：" + e.getMessage());
                }
            }
            return ResultDTO.failure("积分发放失败：重试次数耗尽");
        });
    }



    /**
     * 参数校验
     */
    private void validateParams(Long userId, Long amount, String orderNo) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("PARAM_ERROR", "用户ID必须为正数");
        }
        if (amount == null || amount <= 0) {
            throw new BusinessException("PARAM_ERROR", "积分数量必须大于0");
        }
        if (amount > 1000000) { // 假设最大积分数量为100万
            throw new BusinessException("PARAM_ERROR", "积分数量不能超过100万");
        }
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BusinessException("PARAM_ERROR", "订单号不能为空");
        }
        if (orderNo.length() > 64) {
            throw new BusinessException("PARAM_ERROR", "订单号长度不能超过64位");
        }
    }

    @DS("master0")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<String> addPointInMaster0(Long userId, Long amount, String orderNo, Integer expireDays) {
        return doAddPoint(userId, amount, orderNo, expireDays, "master0");
    }

    @DS("master1")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<String> addPointInMaster1(Long userId, Long amount, String orderNo, Integer expireDays) {
        return doAddPoint(userId, amount, orderNo, expireDays, "master1");
    }

    private ResultDTO<String> doAddPoint(Long userId, Long amount, String orderNo, Integer expireDays, String dataSource) {
        log.info("开始发放积分 - 用户ID: {}, 积分数量: {}, 订单号: {}, 过期天数: {}, 数据源: {}",
                userId, amount, orderNo, expireDays, dataSource);

        try {
            // 检查订单是否已经处理过（幂等性预检查）
            if (isOrderProcessed(orderNo)) {
                log.warn("订单已处理过 - 用户ID: {}, 订单号: {}, 数据源: {}", userId, orderNo, dataSource);
                return ResultDTO.success("积分发放成功", "该订单已处理过");
            }

            // 乐观锁实现，带重试机制
            int retryCount = 3;
            while (retryCount > 0) {
                // 查询最新积分和version（普通查询，不加锁）
                Point point = pointMapper.selectByUserId(userId);
                if (point == null) {
                    // 如果用户积分记录不存在，先创建
                    Point newPoint = new Point();
                    newPoint.setUserId(userId);
                    newPoint.setPoint(0L);
                    newPoint.setVersion(0);
                    newPoint.setCreateTime(LocalDateTime.now());
                    newPoint.setUpdateTime(LocalDateTime.now());
                    try {
                        pointMapper.insert(newPoint);
                        point = newPoint;
                        log.info("创建用户积分记录 - 用户ID: {}, 数据源: {}", userId, dataSource);
                    } catch (DuplicateKeyException e) {
                        // 并发插入冲突，重新查询
                        log.warn("并发插入冲突，重新查询用户积分记录 - 用户ID: {}, 数据源: {}", userId, dataSource);
                        point = pointMapper.selectByUserId(userId);
                        if (point == null) {
                            throw new RuntimeException("创建用户积分记录失败");
                        }
                    }
                }

                log.info("查询到当前积分 - 用户ID: {}, 积分: {}, 版本: {}, 订单号: {}, 数据源: {}",
                        userId, point.getPoint(), point.getVersion(), orderNo, dataSource);

                long beforePoint = point.getPoint();
                long afterPoint = beforePoint + amount;

                // 计算过期时间
                LocalDateTime expireTime = null;
                if (expireDays != null && expireDays > 0) {
                    expireTime = LocalDateTime.now().plusDays(expireDays);
                }

                // 使用乐观锁增加积分并更新过期时间
                int rows = pointMapper.addPointWithExpireAndVersion(userId, amount, expireTime, point.getVersion());
                if (rows > 0) {
                    // 记录积分流水
                    PointLog pointLog = new PointLog();
                    pointLog.setUserId(userId);
                    pointLog.setOrderNo(orderNo);
                    pointLog.setChangePoint(amount);
                    pointLog.setBeforePoint(beforePoint);
                    pointLog.setAfterPoint(afterPoint);
                    pointLog.setType(1); // 1-购物返利
                    String remark = "订单返利";
                    if (expireDays != null && expireDays > 0) {
                        remark += "（" + expireDays + "天后过期）";
                    }
                    pointLog.setRemark(remark);
                    pointLog.setCreateTime(LocalDateTime.now());
                    pointLogMapper.insert(pointLog);

                    log.info("积分发放成功 - 用户ID: {}, 发放前积分: {}, 发放后积分: {}, " +
                                    "发放数量: {}, 订单号: {}, 过期时间: {}, 数据源: {}",
                            userId, beforePoint, afterPoint, amount, orderNo, expireTime, dataSource);

                    return ResultDTO.success("积分发放成功");
                } else {
                    // 更新失败，可能是并发冲突，重试
                    log.warn("积分更新失败，可能存在并发冲突，正在重试 - 剩余次数: {}", retryCount - 1);
                    retryCount--;
                    if (retryCount <= 0) {
                        log.error("积分发放失败 - 重试次数耗尽 - 用户ID: {}, 积分数量: {}, 订单号: {}, 数据源: {}",
                                userId, amount, orderNo, dataSource);
                        return ResultDTO.failure("积分发放失败：系统繁忙，请稍后重试");
                    }
                    try {
                        Thread.sleep(50); // 短暂休眠后重试
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }

            return ResultDTO.failure("积分发放失败：重试次数耗尽");

        } catch (DuplicateKeyException e) {
            log.error("唯一索引冲突 - 用户ID: {}, 订单号: {}, 数据源: {}, 错误: {}",
                    userId, orderNo, dataSource, e.getMessage());
            throw e;
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("数据库访问异常 - 用户ID: {}, 订单号: {}, 数据源: {}, 错误: {}",
                    userId, orderNo, dataSource, e.getMessage());
            throw new RuntimeException("数据库操作失败", e);
        } catch (Exception e) {
            log.error("积分发放异常 - 用户ID: {}, 积分数量: {}, 订单号: {}, 数据源: {}, 错误信息: {}",
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
            Long existId = pointLogMapper.selectIdByOrderNo(orderNo);
            return existId != null;
        } catch (Exception e) {
            log.error("检查订单是否已处理失败 - 订单号: {}, 错误: {}", orderNo, e.getMessage());
            return false;
        }
    }
}
