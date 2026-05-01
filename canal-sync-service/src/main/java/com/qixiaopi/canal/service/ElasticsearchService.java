package com.qixiaopi.canal.service;

import com.qixiaopi.canal.entity.ProductIndex;

import java.util.List;

/**
 * Elasticsearch 服务接口
 *
 * <p>功能说明：
 * <ul>
 *   <li>定义 Elasticsearch 数据操作的基本方法</li>
 *   <li>管理商品数据的索引、删除和批量操作</li>
 *   <li>支持按 sku_id 分片的数据路由</li>
 *   <li>处理 ES 节点故障和恢复</li>
 * </ul>
 *
 * <p>数据分片策略：
 * <ul>
 *   <li>根据 sku_id 的哈希值决定目标 ES 节点</li>
 *   <li>目标节点索引 = hash(sku_id) % ES节点数量</li>
 *   <li>确保同一 sku_id 的数据始终路由到同一节点</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * &#64;Autowired
 * private ElasticsearchService elasticsearchService;
 *
 * // 索引单个商品
 * ProductIndex product = ProductIndex.builder()
 *     .id(1L)
 *     .skuId(100001L)
 *     .productName("iPhone 15")
 *     .build();
 * elasticsearchService.indexProduct(product);
 *
 * // 批量索引
 * List&lt;ProductIndex&gt; products = Arrays.asList(product1, product2);
 * elasticsearchService.bulkIndexProducts(products);
 *
 * // 删除商品
 * elasticsearchService.deleteProduct("1");
 * </pre>
 *
 * <p>实现类：
 * <ul>
 *   <li>{@link com.qixiaopi.canal.service.impl.ElasticsearchServiceImpl}</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see com.qixiaopi.canal.service.impl.ElasticsearchServiceImpl
 * @see com.qixiaopi.canal.entity.ProductIndex
 */
public interface ElasticsearchService {

    /**
     * 索引单个商品到 Elasticsearch
     *
     * <p>功能说明：
     * <ul>
     *   <li>将商品数据写入 Elasticsearch</li>
     *   <li>根据 sku_id 的哈希值路由到对应的 ES 节点</li>
     *   <li>如果目标节点不可用，会等待节点恢复后重试</li>
     * </ul>
     *
     * <p>路由算法：
     * <pre>
     * 目标节点索引 = Math.abs(skuId.hashCode()) % ES节点数量
     * </pre>
     *
     * <p>异常处理：
     * <ul>
     *   <li>如果 ES 节点不可用，会记录失败并触发退避重试策略</li>
     *   <li>所有 ES 节点都不可用时，会抛出异常</li>
     * </ul>
     *
     * @param product 要索引的商品信息
     * @throws RuntimeException 当所有 ES 节点都不可用时
     */
    void indexProduct(ProductIndex product);

    /**
     * 从 Elasticsearch 删除商品
     *
     * <p>功能说明：
     * <ul>
     *   <li>根据商品 ID 删除对应的文档</li>
     *   <li>根据商品原始的 sku_id 路由到对应的 ES 节点</li>
     *   <li>如果目标节点不可用，会等待节点恢复后重试</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>删除操作也会占用一次重试次数</li>
     *   <li>删除不存在的文档不会报错（ES 的 delete 是幂等的）</li>
     * </ul>
     *
     * @param productId 要删除的商品 ID（对应 MySQL 的 id 字段）
     * @throws RuntimeException 当所有 ES 节点都不可用时
     */
    void deleteProduct(String productId);

    /**
     * 批量索引商品到 Elasticsearch
     *
     * <p>功能说明：
     * <ul>
     *   <li>将多个商品批量写入 Elasticsearch</li>
     *   <li>每个商品根据其 sku_id 独立路由到对应的 ES 节点</li>
     *   <li>使用 Bulk API 提高批量写入效率</li>
     *   <li>如果某个节点失败，会记录失败并对失败商品进行重试</li>
     * </ul>
     *
     * <p>性能优化：
     * <ul>
     *   <li>使用 Bulk API 减少网络请求次数</li>
     *   <li>按目标节点分组后批量写入，减少连接数</li>
     *   <li>失败重试时使用单条写入，提高成功率</li>
     * </ul>
     *
     * <p>异常处理：
     * <ul>
     *   <li>部分商品失败不会导致整体失败</li>
     *   <li>失败商品会记录日志并触发重试</li>
     *   <li>所有商品都失败时才会抛出异常</li>
     * </ul>
     *
     * @param products 要批量索引的商品列表
     * @throws RuntimeException 当所有 ES 节点都不可用时
     */
    void bulkIndexProducts(List<ProductIndex> products);
}