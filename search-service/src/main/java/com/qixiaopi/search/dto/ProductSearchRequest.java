package com.qixiaopi.search.dto; 

import lombok.Data; 

/**
 * 商品搜索请求DTO
 * 用于接收前端搜索请求参数
 */
@Data 
public class ProductSearchRequest { 
    /**
     * 搜索关键词
     * 用于按商品名称和描述进行搜索
     */
    private String keyword; 
    
    /**
     * 商品分类
     * 用于按分类进行筛选
     */
    private String category; 
    
    /**
     * 商品品牌
     * 用于按品牌进行筛选
     */
    private String brand; 
    
    /**
     * 最低价格（分）
     * 用于价格范围筛选
     */
    private Long minPrice; 
    
    /**
     * 最高价格（分）
     * 用于价格范围筛选
     */
    private Long maxPrice; 
    
    /**
     * 排序字段
     * 例如：sales（销量）、price（价格）等
     */
    private String sortBy; 
    
    /**
     * 排序方向
     * 可选值：asc（升序）、desc（降序）
     */
    private String sortOrder; 
    
    /**
     * 页码
     * 默认为0（第一页）
     */
    private Integer pageNum = 0; 
    
    /**
     * 每页大小
     * 默认为10
     */
    private Integer pageSize = 10; 
}