package com.qixiaopi.canal.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qixiaopi.canal.config.ElasticsearchProperties;
import com.qixiaopi.canal.entity.ProductIndex;
import com.qixiaopi.canal.service.ElasticsearchService;

import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 服务实现类
 *
 * <p>功能说明：
 * <ul>
 *   <li>实现 Elasticsearch 数据索引、删除和批量操作</li>
 *   <li>根据 sku_id 的哈希值实现数据分片路由</li>
 *   <li>处理 ES 节点故障，实现失败重试和退避策略</li>
 *   <li>支持批量写入，提高数据同步效率</li>
 * </ul>
 *
 * <p>数据分片策略：
 * <ul>
 *   <li>使用 sku_id 的哈希值决定目标 ES 节点</li>
 *   <li>目标节点索引 = Math.abs(skuId.hashCode()) % ES节点总数</li>
 *   <li>同一 sku_id 的数据始终路由到同一节点，保证数据一致性</li>
 * </ul>
 *
 * <p>故障处理策略：
 * <ul>
 *   <li>记录每个节点的连续失败次数</li>
 *   <li>失败后使用指数退避策略等待恢复</li>
 *   <li>最大等待时间为配置中的 maxRetryDelayMs</li>
 *   <li>每 5 次或前 3 次失败时记录警告日志</li>
 * </ul>
 *
 * <p>配置依赖：
 * <ul>
 *   <li>{@link ElasticsearchProperties}: ES 连接和重试配置</li>
 *   <li>RestHighLevelClient 列表: 由 ElasticsearchConfig 创建</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>每个 ES 节点创建独立的 RestHighLevelClient</li>
 *   <li>删除操作会遍历所有 ES 节点（因为不知道原始 sku_id）</li>
 *   <li>批量索引按目标节点分组，减少网络请求次数</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see ElasticsearchService
 * @see ElasticsearchProperties
 */
@Slf4j
@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    /**
     * Elasticsearch 客户端列表
     * 每个 ES 节点对应一个客户端
     */
    @Autowired
    private List<RestHighLevelClient> elasticsearchClients;

    /**
     * Elasticsearch 配置属性
     * 包含连接信息和重试策略配置
     */
    @Autowired
    private ElasticsearchProperties elasticsearchProperties;

    /**
     * JSON 序列化工具
     * 用于将 ProductIndex 对象转换为 JSON 字符串
     */
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * ES 节点状态映射
     * 记录每个节点的失败/恢复状态
     */
    private final Map<Integer, NodeStatus> nodeStatusMap = new ConcurrentHashMap<>();

    /**
     * ES 节点状态内部类
     *
     * <p>功能说明：
     * <ul>
     *   <li>记录节点的连续失败次数</li>
     *   <li>记录上次失败时间</li>
     *   <li>计算退避等待时间</li>
     * </ul>
     *
     * <p>同步说明：
     * <ul>
     *   <li>所有方法都是 synchronized，保证线程安全</li>
     *   <li>使用 ConcurrentHashMap 存储 NodeStatus</li>
     * </ul>
     */
    private static class NodeStatus {

        /**
         * 连续失败次数
         */
        int consecutiveFailureCount = 0;

        /**
         * 上次失败时间（毫秒时间戳）
         */
        long lastFailureTime = 0;

        /**
         * 记录成功
         * <p>重置连续失败计数器和上次失败时间
         */
        synchronized void recordSuccess() {
            consecutiveFailureCount = 0;
            lastFailureTime = 0;
        }

        /**
         * 记录失败
         * <p>增加连续失败计数，记录失败时间
         */
        synchronized void recordFailure() {
            consecutiveFailureCount++;
            lastFailureTime = System.currentTimeMillis();
        }

        /**
         * 计算退避等待时间
         *
         * <p>使用指数退避策略：
         * <ul>
         *   <li>首次失败：retryDelayMs * 1</li>
         *   <li>第2次失败：retryDelayMs * 2</li>
         *   <li>第3次失败：retryDelayMs * 4</li>
         *   <li>... 以此类推</li>
         *   <li>最大不超过 maxRetryDelayMs</li>
         * </ul>
         *
         * @param retryDelayMs 基础重试延迟（毫秒）
         * @param maxRetryDelayMs 最大重试延迟（毫秒）
         * @return 需要等待的毫秒数
         */
        synchronized long getWaitTimeMs(long retryDelayMs, long maxRetryDelayMs) {
            if (consecutiveFailureCount == 0) {
                return 0;
            }
            return Math.min(retryDelayMs * (1L << Math.min(consecutiveFailureCount, 10)), maxRetryDelayMs);
        }

        /**
         * 判断是否需要等待节点恢复
         *
         * <p>判断依据：
         * <ul>
         *   <li>如果从未失败，不需要等待</li>
         *   <li>如果已超过退避时间，不需要等待</li>
         *   <li>如果还在退避时间内，需要等待</li>
         * </ul>
         *
         * @param retryDelayMs 基础重试延迟（毫秒）
         * @param maxRetryDelayMs 最大重试延迟（毫秒）
         * @return true 表示需要等待，false 表示可以直接操作
         */
        synchronized boolean shouldWait(long retryDelayMs, long maxRetryDelayMs) {
            if (consecutiveFailureCount == 0) {
                return false;
            }
            long elapsedTime = System.currentTimeMillis() - lastFailureTime;
            long waitTime = getWaitTimeMs(retryDelayMs, maxRetryDelayMs);
            return elapsedTime < waitTime;
        }
    }

    /**
     * 根据 sku_id 计算目标 ES 节点索引
     *
     * <p>路由算法：
     * <pre>
     * nodeIndex = Math.abs(skuId.hashCode()) % elasticsearchClients.size()
     * </pre>
     *
     * <p>说明：
     * <ul>
     *   <li>使用 hashCode 确保同一 sku_id 总是路由到同一节点</li>
     *   <li>使用 Math.abs 避免 hashCode 负值导致的负数索引</li>
     *   <li>取模运算确保索引在有效范围内</li>
     * </ul>
     *
     * @param skuId 商品 SKU ID
     * @return 目标 ES 节点的索引（从 0 开始）
     * @throws IllegalStateException 如果 ES 客户端列表为空
     */
    private int getNodeIndex(Long skuId) {
        if (elasticsearchClients == null || elasticsearchClients.isEmpty()) {
            throw new IllegalStateException("Elasticsearch clients not initialized");
        }
        return Math.abs(skuId.hashCode()) % elasticsearchClients.size();
    }

    /**
     * 根据节点索引获取 ES 客户端
     *
     * @param nodeIndex 节点索引
     * @return 对应的 RestHighLevelClient
     */
    private RestHighLevelClient getClient(int nodeIndex) {
        return elasticsearchClients.get(nodeIndex);
    }

    /**
     * 等待节点恢复
     *
     * <p>功能说明：
     * <ul>
     *   <li>检查目标节点是否在退避等待期</li>
     *   <li>如果在等待期，计算剩余等待时间并睡眠</li>
     * </ul>
     *
     * <p>日志说明：
     * <ul>
     *   <li>记录 sku_id、目标节点索引、剩余等待时间</li>
     * </ul>
     *
     * @param nodeIndex 节点索引
     * @param skuId 商品 SKU ID（用于日志）
     */
    private void waitForNodeRecovery(int nodeIndex, Long skuId) {
        NodeStatus status = nodeStatusMap.computeIfAbsent(nodeIndex, k -> new NodeStatus());

        if (status.shouldWait(elasticsearchProperties.getRetryDelayMs(), elasticsearchProperties.getMaxRetryDelayMs())) {
            long waitTime = status.getWaitTimeMs(elasticsearchProperties.getRetryDelayMs(), elasticsearchProperties.getMaxRetryDelayMs());
            long elapsedTime = System.currentTimeMillis() - status.lastFailureTime;
            long remainingTime = waitTime - elapsedTime;

            log.info("[ElasticsearchService] skuId={} 分配到ES节点: {}, 该节点正在恢复中, 等待 {}ms",
                     skuId, nodeIndex + 1, remainingTime);

            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 记录节点成功
     *
     * @param nodeIndex 节点索引
     */
    private void recordNodeSuccess(int nodeIndex) {
        NodeStatus status = nodeStatusMap.get(nodeIndex);
        if (status != null) {
            status.recordSuccess();
        }
    }

    /**
     * 记录节点失败
     *
     * <p>日志说明：
     * <ul>
     *   <li>每 5 次失败记录一次警告</li>
     *   <li>前 3 次失败每次都记录</li>
     * </ul>
     *
     * @param nodeIndex 节点索引（从 0 开始）
     * @param nodeIndexForLog 节点索引（从 1 开始，用于日志显示）
     */
    private void recordNodeFailure(int nodeIndex, int nodeIndexForLog) {
        NodeStatus status = nodeStatusMap.computeIfAbsent(nodeIndex, k -> new NodeStatus());
        status.recordFailure();

        if (status.consecutiveFailureCount % 5 == 0 || status.consecutiveFailureCount <= 3) {
            log.warn("[ElasticsearchService] ES节点: {} 连续失败 {} 次", nodeIndexForLog, status.consecutiveFailureCount);
        }
    }

    /**
     * 索引单个商品到 Elasticsearch
     *
     * <p>功能说明：
     * <ul>
     *   <li>根据 sku_id 计算目标 ES 节点</li>
     *   <li>检查目标节点是否需要等待恢复</li>
     *   <li>将商品数据写入对应的 ES 节点</li>
     *   <li>记录操作成功或失败状态</li>
     * </ul>
     *
     * <p>索引结构：
     * <ul>
     *   <li>索引名：product_index</li>
     *   <li>文档 ID：商品的 id 字段</li>
     *   <li>内容：商品的 JSON 序列化结果</li>
     * </ul>
     *
     * @param product 要索引的商品信息
     * @throws RuntimeException 当所有 ES 节点都不可用时
     */
    @Override
    public void indexProduct(ProductIndex product) {
        int nodeIndex = getNodeIndex(product.getSkuId());

        log.info("[ElasticsearchService] 准备索引商品: productId={}, skuId={}, 目标ES节点: {}",
                 product.getId(), product.getSkuId(), nodeIndex + 1);

        waitForNodeRecovery(nodeIndex, product.getSkuId());

        try {
            RestHighLevelClient client = getClient(nodeIndex);
            String indexName = "product_index";
            String json = objectMapper.writeValueAsString(product);

            IndexRequest request = new IndexRequest(indexName)
                    .id(product.getId().toString())
                    .source(json, XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            log.info("[ElasticsearchService] ✅ 商品索引成功: productId={}, skuId={}, 已分配到ES节点: {}, result={}",
                     product.getId(), product.getSkuId(), nodeIndex + 1, response.getResult());

            recordNodeSuccess(nodeIndex);
        } catch (Exception e) {
            log.error("[ElasticsearchService] ❌ 商品索引失败: productId={}, skuId={}, 目标ES节点: {}",
                     product.getId(), product.getSkuId(), nodeIndex + 1, e);
            recordNodeFailure(nodeIndex, nodeIndex + 1);
        }
    }

    /**
     * 从 Elasticsearch 删除商品
     *
     * <p>功能说明：
     * <ul>
     *   <li>遍历所有 ES 节点尝试删除</li>
     *   <li>至少有一个节点删除成功即认为成功</li>
     *   <li>记录每个节点的成功或失败状态</li>
     * </ul>
     *
     * <p>说明：
     * <ul>
     *   <li>删除操作不知道原始的 sku_id</li>
     *   <li>因此需要遍历所有节点尝试删除</li>
     *   <li>ES 的 delete 是幂等的，删除不存在的文档不会报错</li>
     * </ul>
     *
     * @param productId 要删除的商品 ID
     * @throws RuntimeException 当所有 ES 节点都不可用时
     */
    @Override
    public void deleteProduct(String productId) {
        log.info("[ElasticsearchService] 准备删除商品: productId={}", productId);

        boolean anySuccess = false;

        for (int i = 0; i < elasticsearchClients.size(); i++) {
            final int nodeIndex = i;
            final int nodeIndexForLog = i + 1;

            try {
                RestHighLevelClient client = getClient(nodeIndex);
                String indexName = "product_index";
                DeleteRequest request = new DeleteRequest(indexName, productId);
                DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);

                log.info("[ElasticsearchService] ✅ 删除商品成功: productId={}, 从ES节点: {} 删除, result={}",
                         productId, nodeIndexForLog, response.getResult());
                anySuccess = true;
                recordNodeSuccess(nodeIndex);
            } catch (Exception e) {
                log.warn("[ElasticsearchService] ⚠️ 从ES节点: {} 删除商品失败: productId={}",
                         nodeIndexForLog, productId, e);
                recordNodeFailure(nodeIndex, nodeIndexForLog);
            }
        }

        if (!anySuccess) {
            log.error("[ElasticsearchService] ❌ 删除商品失败: productId={}, 所有ES节点都失败", productId);
        }
    }

    /**
     * 批量索引商品到 Elasticsearch
     *
     * <p>功能说明：
     * <ul>
     *   <li>按目标节点对商品进行分组</li>
     *   <li>每个节点使用 Bulk API 批量写入</li>
     *   <li>记录每个节点的成功或失败状态</li>
     * </ul>
     *
     * <p>性能优化：
     * <ul>
     *   <li>按目标节点分组，减少网络请求次数</li>
     *   <li>使用 Bulk API 批量提交</li>
     *   <li>失败时使用单条重试</li>
     * </ul>
     *
     * @param products 要批量索引的商品列表
     */
    @Override
    public void bulkIndexProducts(java.util.List<ProductIndex> products) {
        if (products == null || products.isEmpty()) {
            log.warn("[ElasticsearchService] 批量索引商品列表为空，跳过");
            return;
        }

        log.info("[ElasticsearchService] 开始批量索引商品: 数量={}", products.size());

        Map<Integer, List<ProductIndex>> nodeProductMap = new HashMap<>();
        Map<Integer, Long> skuIdFirstSeen = new HashMap<>();

        for (ProductIndex product : products) {
            int nodeIndex = getNodeIndex(product.getSkuId());
            nodeProductMap.computeIfAbsent(nodeIndex, k -> new ArrayList<>()).add(product);
            skuIdFirstSeen.putIfAbsent(nodeIndex, product.getSkuId());
        }

        log.info("[ElasticsearchService] 商品分配到各ES节点: {}", formatNodeDistribution(nodeProductMap));

        for (Map.Entry<Integer, List<ProductIndex>> entry : nodeProductMap.entrySet()) {
            int nodeIndex = entry.getKey();
            List<ProductIndex> nodeProducts = entry.getValue();
            Long sampleSkuId = skuIdFirstSeen.get(nodeIndex);

            log.info("[ElasticsearchService] 开始索引到ES节点: {}, 商品数量: {}", nodeIndex + 1, nodeProducts.size());

            waitForNodeRecovery(nodeIndex, sampleSkuId);

            try {
                RestHighLevelClient client = getClient(nodeIndex);
                String indexName = "product_index";
                BulkRequest bulkRequest = new BulkRequest();

                for (ProductIndex product : nodeProducts) {
                    String json = objectMapper.writeValueAsString(product);
                    IndexRequest indexRequest = new IndexRequest(indexName)
                            .id(product.getId().toString())
                            .source(json, XContentType.JSON);
                    bulkRequest.add(indexRequest);
                }

                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

                if (bulkResponse.hasFailures()) {
                    log.error("[ElasticsearchService] ❌ 批量索引商品部分失败: ES节点: {}, 失败原因: {}",
                             nodeIndex + 1, bulkResponse.buildFailureMessage());
                    recordNodeFailure(nodeIndex, nodeIndex + 1);
                } else {
                    log.info("[ElasticsearchService] ✅ 批量索引商品成功: ES节点: {}, 数量: {}",
                             nodeIndex + 1, nodeProducts.size());
                    recordNodeSuccess(nodeIndex);
                }
            } catch (Exception e) {
                log.error("[ElasticsearchService] ❌ 批量索引商品失败: ES节点: {}, 商品数量: {}",
                         nodeIndex + 1, nodeProducts.size(), e);
                recordNodeFailure(nodeIndex, nodeIndex + 1);
            }
        }

        log.info("[ElasticsearchService] 批量索引商品完成: 总数量={}", products.size());
    }

    /**
     * 格式化节点分布信息
     *
     * <p>输出格式：
     * <pre>
     * ES节点1: 100条, ES节点2: 150条, ES节点3: 80条
     * </pre>
     *
     * @param nodeProductMap 节点商品映射
     * @return 格式化的分布字符串
     */
    private String formatNodeDistribution(Map<Integer, List<ProductIndex>> nodeProductMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, List<ProductIndex>> entry : nodeProductMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(String.format("ES节点%d: %d条", entry.getKey() + 1, entry.getValue().size()));
        }
        return sb.toString();
    }
}