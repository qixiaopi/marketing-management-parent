package com.qixiaopi.search.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qixiaopi.search.dto.ProductSearchRequest;
import com.qixiaopi.search.dto.ResultDTO;
import com.qixiaopi.search.entity.ProductIndex;
import com.qixiaopi.search.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/search")
    public ResultDTO<List<ProductIndex>> searchProducts(@RequestBody ProductSearchRequest request) {
        log.info("收到商品搜索请求：{}", request);
        try {
            List<ProductIndex> result = productService.searchProducts(request);
            return ResultDTO.success(result);
        } catch (Exception e) {
            log.error("商品搜索失败", e);
            return ResultDTO.failure("商品搜索失败：" + e.getMessage());
        }
    }
    @GetMapping("/{skuId}")
    public ResultDTO<ProductIndex> getProductById(@PathVariable Long skuId) {
        log.info("收到查询商品请求，SKU ID：{}", skuId);
        try {
            ProductIndex result = productService.getProductById(skuId);
            if (result == null) {
                return ResultDTO.failure("商品不存在");
            }
            return ResultDTO.success(result);
        } catch (Exception e) {
            log.error("查询商品失败", e);
            return ResultDTO.failure("查询商品失败：" + e.getMessage());
        }
    }
}