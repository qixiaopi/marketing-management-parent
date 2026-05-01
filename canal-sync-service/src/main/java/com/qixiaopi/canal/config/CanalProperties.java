package com.qixiaopi.canal.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Canal 配置属性类
 *
 * <p>功能说明：
 * <ul>
 *   <li>绑定 application.yml 中的 canal 相关配置</li>
 *   <li>管理 Canal Server 连接信息</li>
 *   <li>管理数据过滤规则</li>
 *   <li>管理批次处理参数</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * canal:
 *   server:
 *     destinations: product_db_0,product_db_1
 *     username: canal
 *     password: canal
 *   zkServers: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
 *   filter:
 *     tables: product_db_0.t_product,product_db_1.t_product
 *   batch:
 *     size: 1000
 * </pre>
 *
 * @author qixiaopi
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "canal")
public class CanalProperties {

    /**
     * Canal Server 配置
     */
    private Server server;

    /**
     * ZooKeeper 集群地址
     * 格式：ip:port,ip:port,ip:port
     * 示例：127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
     */
    private String zkServers;

    /**
     * 数据过滤配置
     */
    private Filter filter;

    /**
     * 批次处理配置
     */
    private Batch batch;

    /**
     * Canal Server 配置信息
     *
     * <p>包含：
     * <ul>
     *   <li>destinations: Canal 实例列表（如 product_db_0, product_db_1）</li>
     *   <li>username: Canal 认证用户名</li>
     *   <li>password: Canal 认证密码</li>
     * </ul>
     */
    @Data
    public static class Server {

        /**
         * Canal 实例列表
         * 每个实例对应一个 MySQL 数据库
         */
        private List<String> destinations;

        /**
         * Canal 认证用户名
         */
        private String username;

        /**
         * Canal 认证密码
         */
        private String password;
    }

    /**
     * 数据过滤配置
     *
     * <p>用于指定需要监听的数据表
     * 格式：database.table（多个用逗号分隔）
     */
    @Data
    public static class Filter {

        /**
         * 要监听的表列表
         * 格式：database.table
         * 示例：product_db_0.t_product,product_db_1.t_product
         */
        private List<String> tables;
    }

    /**
     * 批次处理配置
     *
     * <p>用于控制每次从 Canal 获取的数据量
     */
    @Data
    public static class Batch {

        /**
         * 每次获取的最大数据条数
         * 建议值：1000-5000
         * 过小：频繁网络通信，效率低
         * 过大：内存占用高，延迟增加
         */
        private Integer size;
    }
}