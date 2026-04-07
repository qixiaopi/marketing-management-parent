--在 master0 和 master1 中执行

-- master0 (localhost:3306)
CREATE DATABASE IF NOT EXISTS account_db_0 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- master1 (localhost:3308)
CREATE DATABASE IF NOT EXISTS account_db_1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库并创建表
USE account_db_0;

USE account_db_1;



-- 账户表（行锁优化防超扣）
CREATE TABLE IF NOT EXISTS `t_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID（业务主键）',
  `balance` bigint NOT NULL DEFAULT '0' COMMENT '账户余额（单位：分）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

-- 账户流水表（记录所有资金变动）
CREATE TABLE IF NOT EXISTS `t_account_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `order_no` varchar(64) DEFAULT NULL COMMENT '关联订单号',
  `change_amount` bigint NOT NULL COMMENT '变动金额（正数充值，负数扣减）',
  `before_balance` bigint NOT NULL COMMENT '变动前余额',
  `after_balance` bigint NOT NULL COMMENT '变动后余额',
  `type` tinyint NOT NULL COMMENT '类型：1-充值 2-扣减 3-退款',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`), -- 唯一索引，防止重复扣减（幂等性）
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户流水表';

-- 重复请求日志表（记录被拒绝的重复请求）
CREATE TABLE IF NOT EXISTS `t_duplicate_request_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `request_type` varchar(32) NOT NULL COMMENT '请求类型：ACCOUNT_DEDUCT-账户扣减, POINT_ADD-积分发放',
  `reject_reason` varchar(32) NOT NULL COMMENT '拒绝原因：LOCK_FAILED-锁获取失败, DUPLICATE_KEY-唯一索引冲突',
  `request_time` datetime NOT NULL COMMENT '请求时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_request_time` (`request_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重复请求日志表';

-- 在 account_db_0 中删除
DROP TABLE IF EXISTS account_db_0.t_duplicate_request_log ;

-- 在 account_db_1 中删除
DROP TABLE IF EXISTS `t_duplicate_request_log`;

--根据分库规则（user_id % 2）插入数据：
DROP TABLE IF EXISTS account_db_1.t_duplicate_request_log 

--sql
-- 插入到 account_db_0 (master0:3306) - 偶数用户ID
INSERT INTO account_db_0.t_account  (`user_id`, `balance`, `version`) VALUES 
('100', 100000, 0),   -- 1000元
('102', 50000, 0),    -- 500元
('104', 200000, 0);   -- 2000元


-- 插入到 account_db_1 (master1:3308) - 奇数用户ID
INSERT INTO account_db_1.t_account  (`user_id`, `balance`, `version`) VALUES 
('101', 100000, 0),   -- 1000元
('103', 50000, 0),    -- 500元
('105', 200000, 0);   -- 2000元
