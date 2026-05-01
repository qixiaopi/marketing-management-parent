package com.qixiaopi.canal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Canal Sync Service 应用启动类
 *
 * <p>功能说明：
 * <ul>
 *   <li>实时监听 MySQL 数据库的 binlog 变更</li>
 *   <li>将数据同步到 Elasticsearch 集群</li>
 *   <li>支持多 Canal Server 集群模式</li>
 *   <li>支持多 Elasticsearch 节点数据分片</li>
 * </ul>
 *
 * <p>配置说明：
 * <ul>
 *   <li>Canal 配置：连接 ZooKeeper 集群，订阅指定的数据库和表</li>
 *   <li>Elasticsearch 配置：连接多个 ES 节点，按 sku_id 分片存储</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see <a href="https://github.com/alibaba/canal">Canal</a>
 * @see <a href="https://www.elastic.co/">Elasticsearch</a>
 */
@SpringBootApplication
public class CanalSyncApplication {

    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CanalSyncApplication.class, args);
    }
}