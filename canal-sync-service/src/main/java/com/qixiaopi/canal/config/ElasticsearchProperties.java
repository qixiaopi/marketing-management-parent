package com.qixiaopi.canal.config;

import java.util.Arrays;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 配置属性类
 *
 * <p>功能说明：
 * <ul>
 *   <li>绑定 application.yml 中的 spring.elasticsearch 相关配置</li>
 *   <li>管理 Elasticsearch 集群连接信息</li>
 *   <li>管理连接超时和重试策略</li>
 *   <li>支持多个 ES 节点配置</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * spring:
 *   elasticsearch:
 *     uris: http://127.0.0.1:9200,http://127.0.0.1:9201,http://127.0.0.1:9202
 *     username: elastic
 *     password: elastic123
 *     connection-timeout: 5000
 *     socket-timeout: 30000
 *     retry-delay-ms: 1000
 *     max-retry-delay-ms: 60000
 * </pre>
 *
 * @author qixiaopi
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchProperties {

    /**
     * Elasticsearch 节点地址列表
     * 格式：http://host:port,http://host:port,...
     * 示例：http://127.0.0.1:9200,http://127.0.0.1:9201,http://127.0.0.1:9202
     */
    private String uris;

    /**
     * Elasticsearch 认证用户名
     * 如果 ES 没有开启安全认证，可以为空
     */
    private String username;

    /**
     * Elasticsearch 认证密码
     * 如果 ES 没有开启安全认证，可以为空
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     * 建立 TCP 连接的最大等待时间
     * 默认值：5000（5秒）
     */
    private int connectionTimeout;

    /**
     * Socket 超时时间（毫秒）
     * 数据传输的最大等待时间
     * 默认值：30000（30秒）
     */
    private int socketTimeout;

    /**
     * 重试延迟时间（毫秒）
     * ES 节点失败后，首次重试的等待时间
     * 之后会使用指数退避策略
     * 默认值：1000（1秒）
     */
    private long retryDelayMs;

    /**
     * 最大重试延迟时间（毫秒）
     * 指数退避策略的最大等待时间上限
     * 默认值：60000（60秒）
     */
    private long maxRetryDelayMs;

    /**
     * 解析 URIs 字符串为列表
     *
     * <p>将逗号分隔的 URIs 字符串解析为 List
     * 自动去除空白字符
     *
     * @return Elasticsearch 节点地址列表
     * @throws IllegalStateException 如果 uris 为空或未配置
     */
    public List<String> getUriList() {
        if (uris == null || uris.isEmpty()) {
            throw new IllegalStateException("Elasticsearch URIs must be configured");
        }
        return Arrays.asList(uris.split(","));
    }
}