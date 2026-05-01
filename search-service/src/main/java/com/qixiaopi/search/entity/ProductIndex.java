package com.qixiaopi.search.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 商品索引实体类
 * 用于 Elasticsearch 搜索引擎的商品数据索引
 * 
 * @author qixiaopi
 * @since 2026-04-16
 */
@Data
public class ProductIndex {

    /**
     * 主键 ID
     */
    private Long id;
    
    /**
     * SKU ID（库存单位 ID）
     * 用于唯一标识商品规格
     */
    private Long skuId;

    /**
     * 商品名称
     * 支持全文搜索
     */
    private String productName;

    /**
     * 商品描述
     * 支持全文搜索
     */
    private String description;

    /**
     * 商品分类
     * 支持分类筛选
     */
    private String category;

    /**
     * 商品品牌
     * 支持品牌筛选
     */
    private String brand;

    /**
     * 商品价格（单位：分）
     * 支持价格范围筛选和排序
     */
    private Long price;

    /**
     * 商品总库存
     */
    private Integer stock;

    /**
     * 商品销量
     * 支持按销量排序
     */
    private Integer sales;

    /**
     * 商品状态
     * 例如：on_sale（在售）、off_sale（下架）、out_of_stock（缺货）
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}