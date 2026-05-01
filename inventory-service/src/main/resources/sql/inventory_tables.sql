-- 库存库（独立库，避免和订单库抢资源）
CREATE DATABASE IF NOT EXISTS `inventory_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `inventory_db`;

-- 库存表（行锁优化防超卖）
CREATE TABLE IF NOT EXISTS `t_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `available_stock` int NOT NULL DEFAULT '0' COMMENT '可用库存',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`) -- 唯一索引，避免重复sku，同时加速查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

INSERT INTO t_stock (sku_id, available_stock, version) 
VALUES (1, 100, 0);

-- 库存流水表
CREATE TABLE IF NOT EXISTS `t_inventory_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `order_no` varchar(64) DEFAULT NULL COMMENT '关联订单号',
  `change_quantity` int NOT NULL COMMENT '变动数量（正数增加，负数减少）',
  `before_stock` int NOT NULL COMMENT '变动前库存',
  `after_stock` int NOT NULL COMMENT '变动后库存',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';


-- ===========================================================
-- 订单数据库 inventory_db
-- 用于存储 Seata AT 模式的回滚日志
-- ===========================================================
CREATE TABLE inventory_db.undo_log ( 
  `id` bigint NOT NULL AUTO_INCREMENT,  -- 主键，自增
  `branch_id` bigint NOT NULL,  -- 分支事务ID，用于标识分布式事务中的分支
  `xid` varchar(100) NOT NULL,  -- 全局事务ID，唯一标识一个分布式事务
  `context` varchar(128) NOT NULL,  -- 上下文信息，存储额外的业务数据
  `rollback_info` longblob NOT NULL,  -- 回滚信息，存储数据修改前的状态
  `log_status` int NOT NULL,  -- 日志状态：0-正常，1-已提交，2-已回滚
  `log_created` datetime NOT NULL,  -- 日志创建时间
  `log_modified` datetime NOT NULL,  -- 日志修改时间
  PRIMARY KEY (`id`),  -- 主键索引
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)  -- 唯一索引，确保事务和分支的唯一性
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;  -- 使用InnoDB引擎，支持事务，使用utf8mb4字符集

