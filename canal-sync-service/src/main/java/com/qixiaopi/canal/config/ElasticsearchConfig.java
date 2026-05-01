package com.qixiaopi.canal.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 客户端配置类
 *
 * <p>功能说明：
 * <ul>
 *   <li>基于 ElasticsearchProperties 配置创建多个 RestHighLevelClient 实例</li>
 *   <li>每个 ES 节点创建一个独立的客户端</li>
 *   <li>配置连接超时和 Socket 超时参数</li>
 *   <li>支持 HTTP 和 HTTPS 协议</li>
 * </ul>
 *
 * <p>工作流程：
 * <ol>
 *   <li>从 ElasticsearchProperties 读取 ES 节点列表</li>
 *   <li>遍历每个节点 URI，解析 host 和 port</li>
 *   <li>为每个节点创建 RestHighLevelClient</li>
 *   <li>配置连接超时和 socket 超时</li>
 *   <li>返回客户端列表供其他服务使用</li>
 * </ol>
 *
 * <p>注意事项：
 * <ul>
 *   <li>每个 ES 节点创建独立的客户端，客户端之间相互独立</li>
 *   <li>客户端列表按配置顺序排列，索引 0 对应第一个节点</li>
 *   <li>数据分片路由使用 sku_id 的哈希值决定目标节点</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see ElasticsearchProperties
 * @see RestHighLevelClient
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    /**
     * Elasticsearch 配置属性
     */
    @Autowired
    private ElasticsearchProperties elasticsearchProperties;

    /**
     * 创建 Elasticsearch 客户端列表
     *
     * <p>根据配置中的 URIs 列表，为每个 ES 节点创建一个 RestHighLevelClient 实例。
     * 客户端按 URIs 配置顺序排列，索引从 0 开始。
     *
     * <p>超时配置：
     * <ul>
     *   <li>connectionTimeout: 建立 TCP 连接的最大等待时间</li>
     *   <li>socketTimeout: 数据传输的最大等待时间</li>
     * </ul>
     *
     * @return RestHighLevelClient 列表
     */
    @Bean
    public List<RestHighLevelClient> elasticsearchClients() {
        List<RestHighLevelClient> clients = new ArrayList<>();
        List<String> uriList = elasticsearchProperties.getUriList();

        for (String uri : uriList) {
            String[] parts = parseUri(uri.trim());
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            log.info("[ElasticsearchConfig] 初始化 Elasticsearch 客户端: {}:{}", host, port);

            HttpHost[] hosts = new HttpHost[]{new HttpHost(host, port, "http")};
            RestClientBuilder builder = RestClient.builder(hosts);

            builder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder
                            .setConnectTimeout(elasticsearchProperties.getConnectionTimeout())
                            .setSocketTimeout(elasticsearchProperties.getSocketTimeout())
            );

            clients.add(new RestHighLevelClient(builder));

            log.info("[ElasticsearchConfig] Elasticsearch 客户端初始化完成: {}:{}", host, port);
        }

        return clients;
    }

    /**
     * 解析 URI 字符串
     *
     * <p>将完整的 URI 解析为 host 和 port
     * 支持 http:// 和 https:// 前缀
     *
     * @param uri 完整的 URI，如 http://127.0.0.1:9200
     * @return String数组，[0]=host, [1]=port
     */
    private String[] parseUri(String uri) {
        String cleanUri = uri.replace("http://", "").replace("https://", "");
        String[] hostPort = cleanUri.split(":");
        return new String[]{hostPort[0], hostPort[1]};
    }
}