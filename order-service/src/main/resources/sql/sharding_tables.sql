-- 分库分表创建脚本

-- 订单库0（order_db_0）
CREATE DATABASE IF NOT EXISTS `order_db_0` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `order_db_0`;

-- 订单表0
CREATE TABLE IF NOT EXISTS `t_order_0` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表0';

-- 订单表1
CREATE TABLE IF NOT EXISTS `t_order_1` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表1';

-- 订单表2
CREATE TABLE IF NOT EXISTS `t_order_2` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表2';

-- 订单商品表0
CREATE TABLE IF NOT EXISTS `t_order_item_0` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表0';

-- 订单商品表1
CREATE TABLE IF NOT EXISTS `t_order_item_1` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表1';

-- 订单商品表2
CREATE TABLE IF NOT EXISTS `t_order_item_2` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表2';

-- 库存表（独立库，避免和订单库抢资源，行锁优化防超卖）
CREATE TABLE IF NOT EXISTS `t_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `available_stock` int NOT NULL DEFAULT '0' COMMENT '可用库存',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`) -- 唯一索引，避免重复sku，同时加速查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 订单库1（order_db_1）
CREATE DATABASE IF NOT EXISTS `order_db_1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `order_db_1`;

-- 订单表0
CREATE TABLE IF NOT EXISTS `t_order_0` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表0';

-- 订单表1
CREATE TABLE IF NOT EXISTS `t_order_1` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表1';

-- 订单表2
CREATE TABLE IF NOT EXISTS `t_order_2` (
  `id` bigint NOT NULL COMMENT '雪花算法主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（全局唯一，分片键）',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `order_amount` bigint NOT NULL COMMENT '订单金额（分）',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `receive_name` varchar(32) NOT NULL COMMENT '收件人姓名',
  `receive_phone` varchar(11) NOT NULL COMMENT '收件人电话',
  `receive_address` varchar(255) NOT NULL COMMENT '收件地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 全局唯一索引，兜底幂等
  KEY `idx_userid_createtime` (`user_id`,`create_time`), -- 核心查询联合索引，用户+时间筛选，避免回表
  KEY `idx_userid_status` (`user_id`,`order_status`) -- 订单状态筛选联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表2';

-- 订单商品表0
CREATE TABLE IF NOT EXISTS `t_order_item_0` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表0';

-- 订单商品表1
CREATE TABLE IF NOT EXISTS `t_order_item_1` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表1';

-- 订单商品表2
CREATE TABLE IF NOT EXISTS `t_order_item_2` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（分片键）',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sku_name` varchar(255) NOT NULL COMMENT '商品名称',
  `buy_num` int NOT NULL COMMENT '购买数量',
  `price` bigint NOT NULL COMMENT '商品价格（分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表2';

-- 库存表（独立库，避免和订单库抢资源，行锁优化防超卖）
CREATE TABLE IF NOT EXISTS `t_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `available_stock` int NOT NULL DEFAULT '0' COMMENT '可用库存',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`) -- 唯一索引，避免重复sku，同时加速查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';
