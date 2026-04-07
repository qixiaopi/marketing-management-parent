package com.qixiaopi.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redisson.cluster.nodes[0]}")
    private String node1;

    @Value("${redisson.cluster.nodes[1]}")
    private String node2;

    @Value("${redisson.cluster.nodes[2]}")
    private String node3;

    @Value("${redisson.cluster.nodes[3]}")
    private String node4;

    @Value("${redisson.cluster.nodes[4]}")
    private String node5;

    @Value("${redisson.cluster.nodes[5]}")
    private String node6;

    @Value("${redisson.cluster.scan-interval:2000}")
    private int scanInterval;

    @Value("${redisson.cluster.connect-timeout:3000}")
    private int connectTimeout;

    @Value("${redisson.cluster.timeout:3000}")
    private int timeout;

    @Value("${redisson.cluster.retry-attempts:5}")
    private int retryAttempts;

    @Value("${redisson.cluster.retry-interval:1000}")
    private int retryInterval;

    @Value("${redisson.cluster.master-connection-pool-size:64}")
    private int masterConnectionPoolSize;

    @Value("${redisson.cluster.slave-connection-pool-size:64}")
    private int slaveConnectionPoolSize;

    @Value("${redisson.cluster.idle-connection-timeout:10000}")
    private int idleConnectionTimeout;

    @Value("${redisson.cluster.ping-connection-interval:30000}")
    private int pingConnectionInterval;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers()
            .addNodeAddress(node1, node2, node3, node4, node5, node6)
            .setScanInterval(scanInterval)
            .setConnectTimeout(connectTimeout)
            .setTimeout(timeout)
            .setRetryAttempts(retryAttempts)
            .setRetryInterval(retryInterval)
            .setMasterConnectionPoolSize(masterConnectionPoolSize)
            .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
            .setIdleConnectionTimeout(idleConnectionTimeout)
            .setPingConnectionInterval(pingConnectionInterval)
            .setReadMode(org.redisson.config.ReadMode.MASTER)
            .setSubscriptionMode(org.redisson.config.SubscriptionMode.MASTER);
        
        return Redisson.create(config);
    }
}