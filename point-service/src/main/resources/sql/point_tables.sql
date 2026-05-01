-- 创建主库 0
-- master0 (localhost:3306)
CREATE DATABASE IF NOT EXISTS `point_db_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP DATABASE IF EXISTS `point_db_0`;

-- 创建主库 1
-- master1 (localhost:3308)
CREATE DATABASE IF NOT EXISTS `point_db_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE point_db_0;

USE point_db_1;

-- 积分表（行锁优化防并发问题）
CREATE TABLE IF NOT EXISTS `t_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（业务主键）',
  `point` bigint NOT NULL DEFAULT '0' COMMENT '积分余额',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `expire_time` datetime DEFAULT NULL COMMENT '积分过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分表';

-- 积分流水表（记录所有积分变动）
CREATE TABLE IF NOT EXISTS `t_point_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_no` varchar(64) DEFAULT NULL COMMENT '关联订单号',
  `change_point` bigint NOT NULL COMMENT '变动积分（正数增加，负数扣减）',
  `before_point` bigint NOT NULL COMMENT '变动前积分',
  `after_point` bigint NOT NULL COMMENT '变动后积分',
  `type` tinyint NOT NULL COMMENT '类型：1-购物返利 2-活动赠送 3-签到奖励 4-积分扣减',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 唯一索引，防止重复发放（幂等性）
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';

CREATE TABLE IF NOT EXISTS `t_duplicate_request_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `request_type` varchar(32) NOT NULL COMMENT '请求类型',
  `reject_reason` varchar(255) NOT NULL COMMENT '拒绝原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重复请求日志表';


-- 插入到 point_db_0 (master0:3306) - 偶数用户ID
INSERT INTO `t_point` (`user_id`, `point`, `version`) VALUES 
(100, 0, 0),   -- 用户100，初始积分0
(102, 500, 0),  -- 用户102，初始积分500
(104, 1000, 0);  -- 用户104，初始积分1000

-- 插入到 point_db_1 (master1:3308) - 奇数用户ID
INSERT INTO `t_point` (`user_id`, `point`, `version`) VALUES 
(101, 0, 0),   -- 用户101，初始积分0
(103, 500, 0),  -- 用户103，初始积分500
(105, 1000, 0);  -- 用户105，初始积分1000




-- ===========================================================
-- point_db_0
-- 用于存储 Seata AT 模式的回滚日志
-- ===========================================================
CREATE TABLE point_db_0.undo_log ( 
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


-- ===========================================================
-- point_db_1
-- 用于存储 Seata AT 模式的回滚日志
-- ===========================================================
CREATE TABLE point_db_1.undo_log ( 
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

