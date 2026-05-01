package com.qixiaopi.search.service;

import java.util.List;

import com.qixiaopi.search.entity.ProductIndex;

public interface ElasticsearchService {
    void createIndexIfNotExists();
    ProductIndex findById(Long skuId);
    List<ProductIndex> searchWithFilters(String keyword, String category, Long minPrice, Long maxPrice, String sortField, String sortOrder);
}