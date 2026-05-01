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



-- ===========================================================
-- 订单数据库 order_db_0
-- 用于存储 Seata AT 模式的回滚日志
-- ===========================================================
USE `order_db_0`;
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` ( 
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



USE `seata`;
CREATE TABLE IF NOT EXISTS `global_table` (
    `xid`                       VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `transaction_id`            BIGINT COMMENT '事务ID',
    `status`                    TINYINT      NOT NULL COMMENT '事务状态：0-开始，1-提交，2-回滚，3-未知，4-超时，6-完成，13-提交中，14-回滚中',
    `application_id`            VARCHAR(32) COMMENT '应用ID',
    `transaction_service_group` VARCHAR(32) COMMENT '事务分组',
    `transaction_name`          VARCHAR(128) COMMENT '事务名称',
    `timeout`                   INT COMMENT '超时时间（毫秒）',
    `begin_time`                BIGINT COMMENT '开始时间戳',
    `application_data`          VARCHAR(2000) COMMENT '应用数据（JSON格式）',
    `gmt_create`                DATETIME COMMENT '创建时间',
    `gmt_modified`              DATETIME COMMENT '修改时间',
    PRIMARY KEY (`xid`),
    KEY `idx_status_gmt_modified` (`status` , `gmt_modified`) COMMENT '状态和修改时间索引',
    KEY `idx_transaction_id` (`transaction_id`) COMMENT '事务ID索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Seata全局事务表';


CREATE TABLE IF NOT EXISTS `branch_table` (
    `branch_id`         BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`               VARCHAR(128) NOT NULL COMMENT '全局事务ID，关联global_table表',
    `transaction_id`    BIGINT COMMENT '全局事务ID',
    `resource_group_id` VARCHAR(32) COMMENT '资源组ID',
    `resource_id`       VARCHAR(256) COMMENT '资源ID，通常为数据库名或表名',
    `branch_type`       VARCHAR(8) COMMENT '分支类型：AT、TCC、SAGA、XA等',
    `status`            TINYINT COMMENT '分支状态：0-已注册，1-已提交，2-已回滚，3-未知',
    `client_id`         VARCHAR(64) COMMENT '客户端ID',
    `application_data`  VARCHAR(2000) COMMENT '应用数据（JSON格式）',
    `gmt_create`        DATETIME(6) COMMENT '创建时间，精确到微秒',
    `gmt_modified`      DATETIME(6) COMMENT '修改时间，精确到微秒',
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`) COMMENT '全局事务ID索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Seata分支事务表';
  
  
  CREATE TABLE IF NOT EXISTS `distributed_lock` (
    `lock_key`       CHAR(20) NOT NULL COMMENT '锁键名',
    `lock_value`     VARCHAR(20) NOT NULL COMMENT '锁值',
    `expire`         BIGINT COMMENT '过期时间戳',
    primary key (`lock_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Seata分布式锁表';


CREATE TABLE IF NOT EXISTS `lock_table` (
    `row_key`        VARCHAR(128) NOT NULL COMMENT '行锁键，格式：resource_id + table_name + pk',
    `xid`            VARCHAR(128) COMMENT '全局事务ID',
    `transaction_id` BIGINT COMMENT '事务ID',
    `branch_id`      BIGINT       NOT NULL COMMENT '分支事务ID',
    `resource_id`    VARCHAR(256) COMMENT '资源ID，通常为数据库名',
    `table_name`     VARCHAR(32) COMMENT '表名',
    `pk`             VARCHAR(36) COMMENT '主键值',
    `status`         TINYINT      NOT NULL DEFAULT '0' COMMENT '锁状态：0-已锁定，1-回滚中',
    `gmt_create`     DATETIME COMMENT '创建时间',
    `gmt_modified`   DATETIME COMMENT '修改时间',
    PRIMARY KEY (`row_key`),
    KEY `idx_status` (`status`) COMMENT '状态索引',
    KEY `idx_branch_id` (`branch_id`) COMMENT '分支事务ID索引',
    KEY `idx_xid` (`xid`) COMMENT '全局事务ID索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Seata行锁表';

--COMMENT '异步提交锁'
INSERT INTO seata.distributed_lock  (lock_key, lock_value, expire) VALUES ('AsyncCommitting', ' ', 0);
--COMMENT '重试提交锁'
INSERT INTO seata.distributed_lock (lock_key, lock_value, expire) VALUES ('RetryCommitting', ' ', 0);
--COMMENT '重试回滚锁'
INSERT INTO seata.distributed_lock (lock_key, lock_value, expire) VALUES ('RetryRollbacking', ' ', 0);
--COMMENT '事务超时检查锁'
INSERT INTO seata.distributed_lock (lock_key, lock_value, expire) VALUES ('TxTimeoutCheck', ' ', 0);




service.vgroupMapping.core-order-service-tx-group SEATA_GROUP TEXT  default


-- ===========================================================
-- 订单数据库 order_db_1
-- 用于存储 Seata AT 模式的回滚日志
-- ===========================================================
USE `order_db_1`;
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` ( 
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
