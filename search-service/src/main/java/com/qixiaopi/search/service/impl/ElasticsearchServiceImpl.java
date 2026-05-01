package com.qixiaopi.search.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qixiaopi.search.entity.ProductIndex;
import com.qixiaopi.search.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final String INDEX_NAME = "product_index";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.index.number_of_shards:3}")
    private int numberOfShards;

    @Value("${elasticsearch.index.number_of_replicas:1}")
    private int numberOfReplicas;

    private List<String> nodeUrls = new ArrayList<>();
    private AtomicInteger currentNodeIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        log.info("[ElasticsearchServiceImpl] 初始化 Elasticsearch 节点列表...");
        log.info("[ElasticsearchServiceImpl] 配置的 Elasticsearch URIs: {}", elasticsearchUris);
        
        String[] uris = elasticsearchUris.split(",");
        for (String uri : uris) {
            String trimmedUri = uri.trim();
            nodeUrls.add(trimmedUri);
            log.info("[ElasticsearchServiceImpl] 添加 Elasticsearch 节点: {}", trimmedUri);
        }
        
        log.info("[ElasticsearchServiceImpl] Elasticsearch 节点列表初始化完成，共 {} 个节点", nodeUrls.size());
    }

    private String getNextNodeUrl() {
        if (nodeUrls.isEmpty()) {
            log.warn("[ElasticsearchServiceImpl] 节点列表为空，重新初始化...");
            init();
        }
        
        int index = currentNodeIndex.getAndIncrement() % nodeUrls.size();
        if (index < 0) {
            index = 0;
            currentNodeIndex.set(0);
        }
        
        String nodeUrl = nodeUrls.get(index);
        log.debug("[ElasticsearchServiceImpl] 获取下一个节点: {}", nodeUrl);
        return nodeUrl;
    }

    private <T> T executeWithRetry(RetryFunction<T> function) {
        log.debug("[ElasticsearchServiceImpl] 开始执行 Elasticsearch 请求，尝试所有节点...");
        List<String> triedNodes = new ArrayList<>();
        
        for (int i = 0; i < nodeUrls.size(); i++) {
            String nodeUrl = getNextNodeUrl();
            if (triedNodes.contains(nodeUrl)) {
                log.debug("[ElasticsearchServiceImpl] 节点 {} 已尝试过，跳过", nodeUrl);
                continue;
            }
            triedNodes.add(nodeUrl);
            
            try {
                log.debug("[ElasticsearchServiceImpl] 尝试节点: {}", nodeUrl);
                T result = function.execute(nodeUrl);
                log.debug("[ElasticsearchServiceImpl] 节点 {} 执行成功", nodeUrl);
                return result;
            } catch (Exception e) {
                log.error("[ElasticsearchServiceImpl] 节点 {} 执行失败: {}", nodeUrl, e.getMessage(), e);
            }
        }
        
        log.error("[ElasticsearchServiceImpl] 所有 Elasticsearch 节点都不可用，共尝试了 {} 个节点", triedNodes.size());
        throw new RuntimeException("All Elasticsearch nodes are unavailable");
    }

    @FunctionalInterface
    private interface RetryFunction<T> {
        T execute(String nodeUrl) throws Exception;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (!username.isEmpty() && !password.isEmpty()) {
            log.debug("[ElasticsearchServiceImpl] 添加认证信息");
            String auth = username + ":" + password;
            headers.setBasicAuth(auth);
        }
        
        return headers;
    }

    @Override
    public void createIndexIfNotExists() {
        log.info("[ElasticsearchServiceImpl] 检查并创建索引: {}", INDEX_NAME);
        executeWithRetry(nodeUrl -> {
            String url = nodeUrl + "/" + INDEX_NAME;
            log.debug("[ElasticsearchServiceImpl] 检查索引是否存在: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().value() == 200) {
                    log.info("[ElasticsearchServiceImpl] 索引 {} 已存在", INDEX_NAME);
                    return null;
                }
            } catch (Exception e) {
                log.debug("[ElasticsearchServiceImpl] 索引不存在，准备创建: {}", e.getMessage());
            }

            String createIndexJson = String.format("""
                {
                  "settings": {
                    "number_of_shards": %d,
                    "number_of_replicas": %d
                  },
                  "mappings": {
                    "properties": {
                      "id": { "type": "long" },
                      "skuId": { "type": "long" },
                      "productName": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart"
                      },
                      "description": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart"
                      },
                      "category": { "type": "keyword" },
                      "brand": { "type": "keyword" },
                      "price": { "type": "long" },
                      "stock": { "type": "integer" },
                      "sales": { "type": "integer" },
                      "status": { "type": "keyword" },
                      "createTime": { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss" },
                      "updateTime": { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss" }
                    }
                  }
                }
                """, numberOfShards, numberOfReplicas);

            log.debug("[ElasticsearchServiceImpl] 创建索引请求: {}", createIndexJson);
            HttpEntity<String> entity = new HttpEntity<>(createIndexJson, getHeaders());
            restTemplate.put(url, entity);
            log.info("[ElasticsearchServiceImpl] 索引 {} 创建成功", INDEX_NAME);
            return null;
        });
    }

    @Override
    public ProductIndex findById(Long skuId) {
        log.info("[ElasticsearchServiceImpl] 根据 SKU ID 查询商品: {}", skuId);
        return executeWithRetry(nodeUrl -> {
            String url = nodeUrl + "/" + INDEX_NAME + "/_doc/" + skuId;
            log.debug("[ElasticsearchServiceImpl] 查询 URL: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("[ElasticsearchServiceImpl] 查询响应: {}", response.getBody());
            
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("found") && root.get("found").asBoolean()) {
                ProductIndex product = objectMapper.convertValue(root.get("_source"), ProductIndex.class);
                log.info("[ElasticsearchServiceImpl] 找到商品: id={}, skuId={}, name={}, price={}, category={}, brand={}, sales={}, stock={}, status={}", 
                         product.getId(), product.getSkuId(), product.getProductName(), product.getPrice(), 
                         product.getCategory(), product.getBrand(), product.getSales(), product.getStock(), product.getStatus());
                log.debug("[ElasticsearchServiceImpl] 商品详情: description={}, createTime={}, updateTime={}", 
                         product.getDescription(), product.getCreateTime(), product.getUpdateTime());
                return product;
            }
            
            log.warn("[ElasticsearchServiceImpl] 商品不存在，SKU ID: {}", skuId);
            return null;
        });
    }

    @Override
    public List<ProductIndex> searchWithFilters(String keyword, String category, Long minPrice, Long maxPrice,
                                                  String sortField, String sortOrder) {
        log.info("[ElasticsearchServiceImpl] 执行商品搜索: keyword={}, category={}, minPrice={}, maxPrice={}, sortField={}, sortOrder={}", 
                 keyword, category, minPrice, maxPrice, sortField, sortOrder);
        
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{ \"query\": { \"bool\": { \"must\": [");

        if (keyword != null && !keyword.isEmpty()) {
            queryBuilder.append("{ \"match\": { \"productName\": \"").append(keyword).append("\" } }");
        } else {
            queryBuilder.append("{ \"match_all\": {} }");
        }

        queryBuilder.append("], \"filter\": [");

        boolean hasFilter = false;
        if (category != null && !category.isEmpty()) {
            queryBuilder.append("{ \"term\": { \"category\": \"").append(category).append("\" } }");
            hasFilter = true;
        }

        if (minPrice != null || maxPrice != null) {
            if (hasFilter) queryBuilder.append(", ");
            queryBuilder.append("{ \"range\": { \"price\": { ");
            if (minPrice != null) queryBuilder.append("\"gte\": ").append(minPrice);
            if (minPrice != null && maxPrice != null) queryBuilder.append(", ");
            if (maxPrice != null) queryBuilder.append("\"lte\": ").append(maxPrice);
            queryBuilder.append(" } } }");
        }

        queryBuilder.append("] } }");

        if (sortField != null && !sortField.isEmpty()) {
            queryBuilder.append(", \"sort\": [{ \"").append(sortField)
                    .append("\": { \"order\": \"").append(sortOrder != null ? sortOrder : "asc").append("\" } }]");
        }

        queryBuilder.append("}");
        
        String queryJson = queryBuilder.toString();
        log.debug("[ElasticsearchServiceImpl] 搜索查询: {}", queryJson);

        List<ProductIndex> results = executeSearch(queryJson);
        log.info("[ElasticsearchServiceImpl] 搜索完成，找到 {} 个商品", results.size());
        
        // 记录找到的商品详情
        for (int i = 0; i < results.size(); i++) {
            ProductIndex product = results.get(i);
            log.info("[ElasticsearchServiceImpl] 找到商品 {}: id={}, skuId={}, name={}, price={}, category={}, brand={}, sales={}, stock={}, status={}", 
                     (i + 1), product.getId(), product.getSkuId(), product.getProductName(), product.getPrice(), 
                     product.getCategory(), product.getBrand(), product.getSales(), product.getStock(), product.getStatus());
            log.debug("[ElasticsearchServiceImpl] 商品 {} 详情: description={}, createTime={}, updateTime={}", 
                     (i + 1), product.getDescription(), product.getCreateTime(), product.getUpdateTime());
        }
        
        return results;
    }

    private List<ProductIndex> executeSearch(String queryJson) {
        return executeWithRetry(nodeUrl -> {
            List<ProductIndex> results = new ArrayList<>();
            String url = nodeUrl + "/" + INDEX_NAME + "/_search";
            log.debug("[ElasticsearchServiceImpl] 搜索 URL: {}", url);
            
            HttpEntity<String> entity = new HttpEntity<>(queryJson, getHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            log.debug("[ElasticsearchServiceImpl] 搜索响应状态码: {}", response.getStatusCode());
            log.debug("[ElasticsearchServiceImpl] 搜索响应: {}", response.getBody());
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode hits = root.path("hits").path("hits");

            for (JsonNode hit : hits) {
                ProductIndex product = objectMapper.convertValue(hit.get("_source"), ProductIndex.class);
                results.add(product);
                log.debug("[ElasticsearchServiceImpl] 解析到商品: id={}, name={}, price={}, category={}, brand={}", 
                         product.getId(), product.getProductName(), product.getPrice(), 
                         product.getCategory(), product.getBrand());
            }
            
            log.debug("[ElasticsearchServiceImpl] 解析到 {} 个商品", results.size());
            return results;
        });
    }
}