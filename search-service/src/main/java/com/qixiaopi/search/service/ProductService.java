package com.qixiaopi.search.service;

import java.util.List;

import com.qixiaopi.search.dto.ProductSearchRequest;
import com.qixiaopi.search.entity.ProductIndex;

public interface ProductService {
    List<ProductIndex> searchProducts(ProductSearchRequest request);
    ProductIndex getProductById(Long skuId);
}