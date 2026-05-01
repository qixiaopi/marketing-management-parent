package com.qixiaopi.search.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qixiaopi.search.dto.ProductSearchRequest;
import com.qixiaopi.search.entity.ProductIndex;
import com.qixiaopi.search.service.ElasticsearchService;
import com.qixiaopi.search.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Override
    public List<ProductIndex> searchProducts(ProductSearchRequest request) {
        return elasticsearchService.searchWithFilters(
                request.getKeyword(),
                request.getCategory(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getSortBy(),
                request.getSortOrder()
        );
    }

    @Override
    public ProductIndex getProductById(Long skuId) {
        return elasticsearchService.findById(skuId);
    }
}