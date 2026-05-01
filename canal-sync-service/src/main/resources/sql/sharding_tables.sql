-- ===========================================================
-- 商品数据库
| 实例 | 数据库 | 表 | 连接地址 |
|------|--------|-----|----------|
| product_db_0 | product_db_0 | t_product | 127.0.0.1:3306 |
| product_db_1 | product_db_1 | t_product | 127.0.0.1:3308 |
-- ===========================================================
CREATE DATABASE IF NOT EXISTS `product_db_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `product_db_0`;
CREATE DATABASE IF NOT EXISTS `product_db_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `product_db_1`;
-- 商品表 
CREATE TABLE IF NOT EXISTS `t_product` ( 
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', 
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID', 
  `product_name` varchar(255) NOT NULL COMMENT '商品名称', 
  `description` text COMMENT '商品描述', 
  `category` varchar(64) DEFAULT NULL COMMENT '商品分类', 
  `brand` varchar(64) DEFAULT NULL COMMENT '品牌', 
  `price` bigint NOT NULL COMMENT '价格（单位：分）', 
  `stock` int NOT NULL DEFAULT '0' COMMENT '总库存', 
  `sales` int NOT NULL DEFAULT '0' COMMENT '销量', 
  `status` varchar(32) NOT NULL DEFAULT 'on_sale' COMMENT '状态：on_sale-在售，off_sale-下架', 
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', 
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', 
  PRIMARY KEY (`id`), 
  UNIQUE KEY `uk_sku_id` (`sku_id`), 
  KEY `idx_category` (`category`), 
  KEY `idx_brand` (`brand`), 
  KEY `idx_status` (`status`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

INSERT INTO product_db_0.t_product (sku_id, product_name, description, category, brand, price, stock, sales, status) 
VALUES (100001, 'Apple iPhone 14 Pro', '6.1英寸超视网膜XDR显示屏，A16仿生芯片，4800万像素主摄', '手机', 'Apple', 799900, 50, 200, 'on_sale');


INSERT INTO product_db_1.t_product (sku_id, product_name, description, category, brand, price, stock, sales, status) 
VALUES (200001, 'Samsung Galaxy S23 Ultra', '6.8英寸动态AMOLED 2X显示屏，骁龙8 Gen 2处理器，2000万像素主摄', '手机', 'Samsung', 899900, 30, 150, 'on_sale');
