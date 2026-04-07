package com.qixiaopi.order.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.qixiaopi.order.config.DynamicTableNameContext;
import com.qixiaopi.order.dto.ResultDTO;
import com.qixiaopi.order.entity.Order;
import com.qixiaopi.order.entity.OrderCreateMessage;
import com.qixiaopi.order.feign.PayFeignClient;
import com.qixiaopi.order.feign.PointFeignClient;
import com.qixiaopi.order.feign.StockFeignClient;
import com.qixiaopi.order.mapper.OrderMapper;
import com.qixiaopi.order.service.OrderService;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ApplicationContext applicationContext;
    private OrderServiceImpl getProxy() {
        return applicationContext.getBean(OrderServiceImpl.class);
    }
    @Autowired
    private StockFeignClient stockFeignClient;
    @Autowired
    private PayFeignClient payFeignClient;
    @Autowired
    private PointFeignClient pointFeignClient;
    // 核心注解：开启分布式事务，AT模式无侵入业务代码
    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
	public String createOrder(OrderCreateMessage orderMessage) {
    	
            String xid = RootContext.getXID();
            log.info("分布式事务开启，XID：{}", xid);
            Long goodsId = orderMessage.getSkuId();
            Integer num = orderMessage.getNum();
            String orderId = orderMessage.getOrderId();
            String userId = orderMessage.getUserId();
            Long orderAmount = orderMessage.getOrderAmount();
            // 1. 远程调用库存服务，扣减库存（分支事务1）
            ResultDTO<Boolean> stockResult = stockFeignClient.deductStock(goodsId, num, orderId);
            if (!stockResult.isSuccess()) {
                throw new RuntimeException("库存扣减失败");
            }
            // 2. 本地事务：创建订单（分支事务2）
            createOrder(orderId, goodsId, num, userId, orderAmount);
            log.info("订单创建成功，订单号：{}", orderId);

            // 3. 远程调用支付服务，扣减余额（分支事务3）
            String payResult = payFeignClient.deductBalance(userId, orderAmount);
            if (!"success".equals(payResult)) {
                // 抛出异常，触发全局回滚，所有分支事务全部回滚
                throw new RuntimeException("支付扣减失败");
            }

            Long userIdLong = Long.valueOf(userId);
            
            // 4. 远程调用积分服务，发放积分（分支事务4）
            ResultDTO<Boolean> pointResult = pointFeignClient.addPoint(userIdLong, orderAmount.intValue());
            if (!pointResult.isSuccess()) {
                throw new RuntimeException("积分发放失败");
            }

            log.info("分布式事务执行成功，XID：{}", xid);
		return "下单成功";
	}
	

    private String createOrder(String orderId, Long goodsId, Integer num, String userId, Long amount) {
        try {
            Long userIdLong = Long.valueOf(userId);
            
            // 设置用户ID到线程上下文，用于动态表名
            DynamicTableNameContext.setUserId(userIdLong);
            
            Order newOrder = new Order();
            newOrder.setOrderNo(orderId);
            newOrder.setUserId(userIdLong);
            newOrder.setSkuId(goodsId);
            newOrder.setBuyNum(num);
            newOrder.setOrderAmount(amount);
            newOrder.setOrderStatus(0);
            newOrder.setReceiveName("默认收件人");
            newOrder.setReceivePhone("13800138000");
            newOrder.setReceiveAddress("默认收件地址");
            newOrder.setCreateTime(LocalDateTime.now());
            
            // 根据user_id选择数据源：user_id % 2
            String result;
            if (userIdLong % 2 == 0) {
                result = getProxy().insertOrderInMaster0(newOrder);
            } else {
                result = getProxy().insertOrderInMaster1(newOrder);
            }
            
            return result;
        } catch (Exception e) {
            log.error("订单创建失败，订单号：{}", orderId, e);
            return "订单创建失败：" + e.getMessage();
        } finally {
            // 清理线程上下文
            DynamicTableNameContext.clear();
        }
	}
    
    @DS("master0")
    public String insertOrderInMaster0(Order order) {
        try {
            // 计算动态表名用于日志打印
            String tableName = "t_order_" + (order.getUserId() % 3);
            // 使用MyBatis Plus的动态表名功能，不再需要手动指定表名
            orderMapper.insert(order);
            log.info("订单创建成功，订单号：{}，数据源：{}，表名：{}", order.getOrderNo(), "master0", tableName);
            log.info("【订单服务】订单创建成功，订单号：{}", order.getOrderNo());
            return "订单创建成功，订单号：" + order.getOrderNo();
        } catch (Exception e) {
            log.error("订单创建失败，订单号：{}", order.getOrderNo(), e);
            return "订单创建失败：" + e.getMessage();
        }
    }
    
    @DS("master1")
    public String insertOrderInMaster1(Order order) {
        try {
            // 计算动态表名用于日志打印
            String tableName = "t_order_" + (order.getUserId() % 3);
            // 使用MyBatis Plus的动态表名功能，不再需要手动指定表名
            orderMapper.insert(order);
            log.info("订单创建成功，订单号：{}，数据源：{}，表名：{}", order.getOrderNo(), "master1", tableName);
            log.info("【订单服务】订单创建成功，订单号：{}", order.getOrderNo());
            return "订单创建成功，订单号：" + order.getOrderNo();
        } catch (Exception e) {
            log.error("订单创建失败，订单号：{}", order.getOrderNo(), e);
            return "订单创建失败：" + e.getMessage();
        }
    }
}
