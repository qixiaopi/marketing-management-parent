package com.qixiaopi.canal.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品索引实体类
 *
 * <p>功能说明：
 * <ul>
 *   <li>映射 Elasticsearch 中的 product_index 索引结构</li>
 *   <li>用于存储商品的全量信息</li>
 *   <li>支持 JSON 序列化和反序列化</li>
 * </ul>
 *
 * <p>索引结构：
 * <pre>
 * {
 *   "id": 1,
 *   "sku_id": 100001,
 *   "product_name": "iPhone 15 Pro",
 *   "description": "6.1英寸 Super Retina XDR 显示屏",
 *   "category": "手机",
 *   "brand": "Apple",
 *   "price": 799900,
 *   "stock": 100,
 *   "sales": 500,
 *   "status": "on_sale",
 *   "create_time": "2024-01-01T10:00:00",
 *   "update_time": "2024-01-15T14:30:00"
 * }
 * </pre>
 *
 * <p>注意事项：
 * <ul>
 *   <li>id: 商品主键ID，必须唯一</li>
 *   <li>sku_id: 商品SKU ID，用于ES节点分片路由</li>
 *   <li>price: 价格，单位为分（避免浮点数精度问题）</li>
 *   <li>create_time 和 update_time 使用 ISO 格式的日期时间字符串</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIndex {

    /**
     * 商品主键ID
     * 必须唯一，对应 MySQL 中的 id 字段
     */
    private Long id;

    /**
     * 商品SKU ID
     * 用于 Elasticsearch 节点分片路由
     * 根据 sku_id 的哈希值决定数据存储到哪个 ES 节点
     */
    private Long skuId;

    /**
     * 商品名称
     * 示例：iPhone 15 Pro
     */
    private String productName;

    /**
     * 商品描述
     * 详细的产品介绍信息
     */
    private String description;

    /**
     * 商品分类
     * 示例：手机、电脑、平板
     */
    private String category;

    /**
     * 商品品牌
     * 示例：Apple、Samsung、Huawei
     */
    private String brand;

    /**
     * 商品价格
     * 单位：分（避免浮点数精度问题）
     * 示例：799900 表示 7999.00 元
     */
    private Long price;

    /**
     * 商品库存数量
     */
    private Integer stock;

    /**
     * 商品销量
     */
    private Integer sales;

    /**
     * 商品状态
     * 可选值：on_sale（上架）、off_sale（下架）、deleted（删除）
     */
    private String status;

    /**
     * 创建时间
     * 格式：yyyy-MM-dd'T'HH:mm:ss
     * 示例：2024-01-01T10:00:00
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 格式：yyyy-MM-dd'T'HH:mm:ss
     * 示例：2024-01-15T14:30:00
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updateTime;
}