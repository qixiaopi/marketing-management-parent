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
import com.qixiaopi.order.exception.BusinessException;
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
    @GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 60000)
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
            log.info("调用库存服务前，当前XID：{}", xid);
            // 手动设置XID到请求头
            System.setProperty("seata.xid", xid);
            log.info("手动设置XID到系统属性：{}", xid);
            ResultDTO<Boolean> stockResult = stockFeignClient.deductStock(goodsId, num, orderId);
            if (!stockResult.isSuccess()) {
                log.error("库存扣减失败，XID：{}", xid);
                throw new BusinessException("库存扣减失败");
            }
            
            // 2. 本地事务：创建订单（分支事务2）
            String orderResult = createOrder(orderId, goodsId, num, userId, orderAmount);
            if (!orderResult.startsWith("订单创建成功")) {
                log.error("订单创建失败，XID：{}", xid);
                throw new BusinessException(orderResult);
            }
            log.info("订单创建成功，订单号：{}，XID：{}", orderId, xid);
            
            // 3. 远程调用支付服务，扣减余额（分支事务3）
            ResultDTO<String> payResult = payFeignClient.deductBalance(userId, orderAmount, orderId);
            if (!payResult.isSuccess()) {
                log.error("支付扣减失败，XID：{}", xid);
                throw new BusinessException("支付扣减失败：" + payResult.getMessage());
            }

            Long userIdLong = Long.valueOf(userId);
            
            // 4. 远程调用积分服务，发放积分（分支事务4）
            ResultDTO<String> pointResult = pointFeignClient.addPoint(userIdLong, orderAmount, orderId, null);
            if (!pointResult.isSuccess()) {
                log.error("积分发放失败，XID：{}", xid);
                throw new BusinessException("积分发放失败");
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
            return "订单创建失败";
        } finally {
            // 清理线程上下文
            DynamicTableNameContext.clear();
        }
	}
    
    @DS("master0")
    public String insertOrderInMaster0(Order order) {
        // 计算动态表名用于日志打印
        String tableName = "t_order_" + (order.getUserId() % 3);
        // 使用MyBatis Plus的动态表名功能，不再需要手动指定表名
        orderMapper.insert(order);
        log.info("订单创建成功，订单号：{}，数据源：{}，表名：{}", order.getOrderNo(), "master0", tableName);
        log.info("【订单服务】订单创建成功，订单号：{}", order.getOrderNo());
        return "订单创建成功，订单号：" + order.getOrderNo();
    }
    
    @DS("master1")
    public String insertOrderInMaster1(Order order) {
        // 计算动态表名用于日志打印
        String tableName = "t_order_" + (order.getUserId() % 3);
        // 使用MyBatis Plus的动态表名功能，不再需要手动指定表名
        orderMapper.insert(order);
        log.info("订单创建成功，订单号：{}，数据源：{}，表名：{}", order.getOrderNo(), "master1", tableName);
        log.info("【订单服务】订单创建成功，订单号：{}", order.getOrderNo());
        return "订单创建成功，订单号：" + order.getOrderNo();
    }
}
