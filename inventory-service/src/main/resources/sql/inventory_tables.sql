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
