package com.qixiaopi.canal.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.qixiaopi.canal.config.CanalProperties;
import com.qixiaopi.canal.entity.ProductIndex;
import com.qixiaopi.canal.service.CanalSyncService;
import com.qixiaopi.canal.service.ElasticsearchService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Canal 同步服务实现类
 *
 * <p>功能说明：
 * <ul>
 *   <li>连接 Canal Server 集群，实时监听 MySQL binlog 变更</li>
 *   <li>解析 Canal 消息，提取数据变更内容</li>
 *   <li>将数据同步到 Elasticsearch 集群</li>
 *   <li>支持多 destination（多数据库）并行处理</li>
 * </ul>
 *
 * <p>工作流程：
 * <ol>
 *   <li>应用启动时，通过 ZooKeeper 发现可用的 Canal Server 节点</li>
 *   <li>连接到 Canal Server，创建多个连接（每个 destination 一个）</li>
 *   <li>订阅配置的表（默认监听 product_db_0.t_product 和 product_db_1.t_product）</li>
 *   <li>循环拉取 Canal 消息，解析 binlog 变更</li>
 *   <li>将 INSERT/UPDATE 操作转换为 ProductIndex 对象，批量写入 ES</li>
 *   <li>将 DELETE 操作根据 sku_id 路由到对应 ES 节点删除</li>
 *   <li>定期 ACK 已处理的消息，防止消息堆积</li>
 * </ol>
 *
 * <p>数据分片逻辑：
 * <ul>
 *   <li>每个 destination 对应一个独立的连接和线程</li>
 *   <li>数据同步时根据 sku_id 决定目标 ES 节点</li>
 *   <li>目标节点索引 = Math.abs(skuId.hashCode()) % ES节点数量</li>
 * </ul>
 *
 * <p>配置依赖：
 * <ul>
 *   <li>{@link CanalProperties}: Canal 连接和订阅配置</li>
 *   <li>{@link ElasticsearchService}: ES 数据写入服务</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>服务在应用启动时自动启动（通过 @PostConstruct）</li>
 *   <li>服务在应用关闭时自动停止（通过 @PreDestroy）</li>
 *   <li>每个 destination 都有独立的线程处理，互不影响</li>
 *   <li>支持动态配置变更（重启后生效）</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see CanalSyncService
 * @see CanalProperties
 * @see ElasticsearchService
 */
@Slf4j
@Service
public class CanalSyncServiceImpl implements CanalSyncService {

    /**
     * Canal 配置属性
     * 包含连接信息、订阅配置等
     */
    @Autowired
    private CanalProperties canalProperties;

    /**
     * Elasticsearch 服务
     * 用于写入和删除商品数据
     */
    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Canal 连接器列表
     * 每个 destination 对应一个连接器
     */
    private List<CanalConnector> canalConnectors;

    /**
     * 服务运行状态标志
     * true: 运行中
     * false: 已停止
     */
    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 同步线程列表
     * 每个 destination 对应一个线程
     */
    private List<Thread> syncThreads;

    /**
     * 日期时间格式化器
     * 用于解析 MySQL 的 datetime 字段
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化方法
     *
     * <p>在 Bean 创建后自动调用
     * 启动 Canal 同步服务
     */
    @PostConstruct
    public void init() {
        log.info("[CanalSyncService] 初始化 Canal 同步服务...");
        start();
    }

    /**
     * 销毁方法
     *
     * <p>在应用关闭前自动调用
     * 停止 Canal 同步服务
     */
    @PreDestroy
    public void destroy() {
        log.info("[CanalSyncService] 关闭 Canal 同步服务...");
        stop();
    }

    /**
     * 启动 Canal 同步服务
     *
     * <p>功能说明：
     * <ul>
     *   <li>从配置中获取所有 destination 列表</li>
     *   <li>为每个 destination 创建一个 ZooKeeper 集群连接</li>
     *   <li>为每个 destination 启动一个独立的同步线程</li>
     * </ul>
     *
     * <p>连接流程：
     * <ol>
     *   <li>调用 CanalConnectors.newClusterConnector() 创建集群连接器</li>
     *   <li>连接器内部通过 ZooKeeper 发现可用的 Canal Server</li>
     *   <li>创建新线程执行 syncData() 方法</li>
     * </ol>
     *
     * <p>异常处理：
     * <ul>
     *   <li>如果 destinations 为空，抛出 IllegalStateException</li>
     *   <li>如果 Canal Server 不可用，线程会等待重连</li>
     * </ul>
     *
     * @throws IllegalStateException 如果 destinations 未配置
     */
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("[CanalSyncService] 启动 Canal 同步服务...");

            List<String> destinations = canalProperties.getServer().getDestinations();
            if (destinations == null || destinations.isEmpty()) {
                throw new IllegalStateException("Canal destinations not configured");
            }

            log.info("[CanalSyncService] 配置的 destinations: {}", destinations);

            canalConnectors = new ArrayList<>();
            syncThreads = new ArrayList<>();

            for (String destination : destinations) {
                log.info("[CanalSyncService] 初始化 destination: {}", destination);

                CanalConnector connector = CanalConnectors.newClusterConnector(
                        canalProperties.getZkServers(),
                        destination,
                        canalProperties.getServer().getUsername(),
                        canalProperties.getServer().getPassword()
                );

                canalConnectors.add(connector);

                Thread thread = new Thread(() -> syncData(connector, destination),
                                         "canal-sync-thread-" + destination);
                syncThreads.add(thread);
                thread.start();
            }

            log.info("[CanalSyncService] Canal 同步服务启动成功，监控 {} 个 destination", destinations.size());
        }
    }

    /**
     * 停止 Canal 同步服务
     *
     * <p>功能说明：
     * <ul>
     *   <li>设置 running 标志为 false，停止所有同步线程</li>
     *   <li>中断所有同步线程</li>
     *   <li>断开所有 Canal 连接</li>
     *   <li>等待线程安全退出（最多等待 5 秒）</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>如果服务未启动，此方法不会有任何效果</li>
     *   <li>使用 thread.join() 等待线程结束</li>
     *   <li>如果在等待过程中被中断，会记录警告日志</li>
     * </ul>
     */
    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("[CanalSyncService] 停止 Canal 同步服务...");

            if (syncThreads != null) {
                for (Thread thread : syncThreads) {
                    thread.interrupt();
                    try {
                        thread.join(5000);
                    } catch (InterruptedException e) {
                        log.warn("[CanalSyncService] 等待同步线程停止时被中断", e);
                    }
                }
            }

            if (canalConnectors != null) {
                for (CanalConnector connector : canalConnectors) {
                    connector.disconnect();
                }
            }

            log.info("[CanalSyncService] Canal 同步服务已停止");
        }
    }

    /**
     * 检查服务是否正在运行
     *
     * @return true 表示服务正在运行，false 表示已停止
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 同步数据的主循环
     *
     * <p>功能说明：
     * <ul>
     *   <li>连接到 Canal Server</li>
     *   <li>订阅配置的表</li>
     *   <li>循环拉取并处理消息</li>
     * </ul>
     *
     * <p>处理流程：
     * <ol>
     *   <li>connector.connect() - 建立连接</li>
     *   <li>connector.subscribe() - 订阅表</li>
     *   <li>while(running) - 持续拉取消息</li>
     *   <li>connector.getWithoutAck() - 拉取消息（不 ACK）</li>
     *   <li>processEntries() - 处理消息内容</li>
     *   <li>connector.ack() - 确认消息已处理</li>
     * </ol>
     *
     * <p>异常处理：
     * <ul>
     *   <li>处理异常时调用 connector.rollback() 回滚位置</li>
     *   <li>断开连接时在 finally 块中确保资源释放</li>
     * </ul>
     *
     * @param connector Canal 连接器
     * @param destination 目标数据库标识
     */
    private void syncData(CanalConnector connector, String destination) {
        log.info("[CanalSyncService] 同步线程启动，开始监听 {} 的数据变更...", destination);

        try {
            connector.connect();

            String filter = String.join(",", canalProperties.getFilter().getTables());
            connector.subscribe(filter);
            log.info("[CanalSyncService] {} 订阅表: {}", destination, filter);

            while (running.get()) {
                try {
                    Message message = connector.getWithoutAck(canalProperties.getBatch().getSize());
                    long batchId = message.getId();
                    int size = message.getEntries().size();

                    if (batchId != -1 && size > 0) {
                        log.debug("[CanalSyncService] {} 收到数据变更: batchId={}, size={}", destination, batchId, size);

                        processEntries(message.getEntries());
                    }

                    connector.ack(batchId);

                } catch (Exception e) {
                    log.error("[CanalSyncService] {} 处理数据变更时发生错误", destination, e);
                    connector.rollback();
                }
            }
        } catch (Exception e) {
            log.error("[CanalSyncService] {} Canal 连接异常", destination, e);
        } finally {
            if (connector != null) {
                connector.disconnect();
            }
        }
    }

    /**
     * 处理 Canal 条目列表
     *
     * <p>功能说明：
     * <ul>
     *   <li>遍历所有 Canal Entry</li>
     *   <li>解析 RowData 数据</li>
     *   <li>根据事件类型分为 INSERT/UPDATE 和 DELETE</li>
     *   <li>分别调用 ES 服务进行索引或删除</li>
     * </ul>
     *
     * <p>过滤条件：
     * <ul>
     *   <li>只处理表名为 t_product 的数据</li>
     *   <li>只处理库名为 product_db_0 或 product_db_1 的数据</li>
     *   <li>忽略其他表或库的数据</li>
     * </ul>
     *
     * <p>数据分组：
     * <ul>
     *   <li>INSERT/UPDATE 合并为批量索引列表</li>
     *   <li>DELETE 合并为批量删除列表</li>
     *   <li>最后统一调用 ES 服务处理</li>
     * </ul>
     *
     * @param entries Canal 条目列表
     */
    private void processEntries(List<CanalEntry.Entry> entries) {
        List<ProductIndex> insertOrUpdateProducts = new ArrayList<>();
        List<String> deleteProductIds = new ArrayList<>();

        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                CanalEntry.RowChange rowChange;
                try {
                    rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                } catch (Exception e) {
                    log.error("[CanalSyncService] 解析 RowChange 失败", e);
                    continue;
                }

                CanalEntry.EventType eventType = rowChange.getEventType();
                String tableName = entry.getHeader().getTableName();
                String schemaName = entry.getHeader().getSchemaName();

                log.debug("[CanalSyncService] 处理表: {}.{}, 事件类型: {}", schemaName, tableName, eventType);

                if ("t_product".equals(tableName) && ("product_db_0".equals(schemaName) || "product_db_1".equals(schemaName))) {
                    log.info("[CanalSyncService] ✅ 表名和库名匹配，开始处理数据...");
                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        switch (eventType) {
                            case INSERT:
                            case UPDATE:
                                ProductIndex product = parseProduct(rowData.getAfterColumnsList());
                                if (product != null) {
                                    insertOrUpdateProducts.add(product);
                                    log.info("[CanalSyncService] 从数据库 {} 获取商品变更: productId={}, skuId={}, name={}, event={}",
                                             schemaName, product.getId(), product.getSkuId(), product.getProductName(), eventType);
                                } else {
                                    log.warn("[CanalSyncService] parseProduct 返回 null，无法解析商品数据");
                                }
                                break;
                            case DELETE:
                                String productId = parseProductId(rowData.getBeforeColumnsList());
                                if (productId != null) {
                                    deleteProductIds.add(productId);
                                    log.info("[CanalSyncService] 从数据库 {} 获取商品删除: productId={}", schemaName, productId);
                                }
                                break;
                            default:
                                log.debug("[CanalSyncService] 忽略事件类型: {}", eventType);
                        }
                    }
                } else {
                    log.info("[CanalSyncService] ❌ 表名或库名不匹配: tableName={}, schemaName={}, 期望: t_product 且 (product_db_0 或 product_db_1)",
                             tableName, schemaName);
                }
            }
        }

        log.info("[CanalSyncService] 数据处理完成, 待同步: insert/update={}, delete={}",
                 insertOrUpdateProducts.size(), deleteProductIds.size());

        if (!insertOrUpdateProducts.isEmpty()) {
            log.info("[CanalSyncService] 开始批量索引 {} 条商品...", insertOrUpdateProducts.size());
            elasticsearchService.bulkIndexProducts(insertOrUpdateProducts);
        }

        for (String productId : deleteProductIds) {
            log.info("[CanalSyncService] 开始删除商品: productId={}", productId);
            elasticsearchService.deleteProduct(productId);
        }
    }

    /**
     * 解析商品数据
     *
     * <p>功能说明：
     * <ul>
     *   <li>遍历列列表，提取各字段值</li>
     *   <li>将列名与字段映射，构建 ProductIndex 对象</li>
     *   <li>解析价格、库存等数值字段</li>
     *   <li>解析日期时间字段</li>
     * </ul>
     *
     * <p>字段映射：
     * <ul>
     *   <li>id → product.id</li>
     *   <li>sku_id → product.skuId</li>
     *   <li>product_name → product.productName</li>
     *   <li>description → product.description</li>
     *   <li>category → product.category</li>
     *   <li>brand → product.brand</li>
     *   <li>price → product.price（分）</li>
     *   <li>stock → product.stock</li>
     *   <li>sales → product.sales</li>
     *   <li>status → product.status</li>
     *   <li>create_time → product.createTime</li>
     *   <li>update_time → product.updateTime</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>只处理非空字段</li>
     *   <li>必须包含 id 字段，否则返回 null</li>
     *   <li>日期格式必须为 yyyy-MM-dd HH:mm:ss</li>
     * </ul>
     *
     * @param columns Canal 列列表（INSERT/UPDATE 使用 afterColumns）
     * @return 解析后的 ProductIndex 对象，如果解析失败返回 null
     */
    private ProductIndex parseProduct(List<CanalEntry.Column> columns) {
        try {
            ProductIndex product = new ProductIndex();

            for (CanalEntry.Column column : columns) {
                String name = column.getName();
                String value = column.getValue();

                if (value == null || value.isEmpty()) {
                    continue;
                }

                switch (name) {
                    case "id":
                        product.setId(Long.parseLong(value));
                        break;
                    case "sku_id":
                        product.setSkuId(Long.parseLong(value));
                        break;
                    case "product_name":
                        product.setProductName(value);
                        break;
                    case "description":
                        product.setDescription(value);
                        break;
                    case "category":
                        product.setCategory(value);
                        break;
                    case "brand":
                        product.setBrand(value);
                        break;
                    case "price":
                        product.setPrice(Long.parseLong(value));
                        break;
                    case "stock":
                        product.setStock(Integer.parseInt(value));
                        break;
                    case "sales":
                        product.setSales(Integer.parseInt(value));
                        break;
                    case "status":
                        product.setStatus(value);
                        break;
                    case "create_time":
                        product.setCreateTime(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
                        break;
                    case "update_time":
                        product.setUpdateTime(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
                        break;
                }
            }

            return product.getId() != null ? product : null;
        } catch (Exception e) {
            log.error("[CanalSyncService] 解析商品数据失败", e);
            return null;
        }
    }

    /**
     * 解析商品ID（用于删除操作）
     *
     * <p>功能说明：
     * <ul>
     *   <li>遍历列列表，查找 id 字段</li>
     *   <li>返回 id 列的值（字符串形式）</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>DELETE 操作使用 beforeColumns（删除前的数据）</li>
     *   <li>只返回 id 字段的值，不返回 sku_id</li>
     *   <li>因为是删除操作，不需要 sku_id 来确定目标节点</li>
     * </ul>
     *
     * @param columns Canal 列列表（DELETE 使用 beforeColumns）
     * @return 商品 ID 字符串，如果未找到返回 null
     */
    private String parseProductId(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            if ("id".equals(column.getName())) {
                return column.getValue();
            }
        }
        return null;
    }
}