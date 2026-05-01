package com.qixiaopi.canal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qixiaopi.canal.service.CanalSyncService;

/**
 * 健康检查和状态监控控制器
 *
 * <p>功能说明：
 * <ul>
 *   <li>提供应用健康检查接口</li>
 *   <li>提供 Canal 同步服务状态查询</li>
 * </ul>
 *
 * <p>接口列表：
 * <ul>
 *   <li>GET /health - 返回 "OK"，用于基础健康检查</li>
 *   <li>GET /health/status - 返回 Canal 同步服务运行状态</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>K8s 存活探针（liveness probe）检查应用是否存活</li>
 *   <li>K8s 就绪探针（readiness probe）检查服务是否就绪</li>
 *   <li>运维监控检查 Canal 同步状态</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see CanalSyncService
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * Canal 同步服务
     */
    @Autowired
    private CanalSyncService canalSyncService;

    /**
     * 基础健康检查
     *
     * <p>功能说明：
     * <ul>
     *   <li>返回固定字符串 "OK"</li>
     *   <li>用于判断应用进程是否存活</li>
     * </ul>
     *
     * <p>使用场景：
     * <ul>
     *   <li>K8s liveness probe</li>
     *   <li>负载均衡器健康检查</li>
     * </ul>
     *
     * @return 字符串 "OK"
     */
    @GetMapping
    public String health() {
        return "OK";
    }

    /**
     * Canal 同步服务状态查询
     *
     * <p>功能说明：
     * <ul>
     *   <li>检查 Canal 同步服务是否正在运行</li>
     *   <li>返回服务运行状态描述</li>
     * </ul>
     *
     * <p>返回值：
     * <ul>
     *   <li>如果服务运行中：返回 "Canal sync service is running"</li>
     *   <li>如果服务已停止：返回 "Canal sync service is stopped"</li>
     * </ul>
     *
     * <p>使用场景：
     * <ul>
     *   <li>K8s readiness probe</li>
     *   <li>运维监控系统检查同步状态</li>
     *   <li>手动排查服务状态</li>
     * </ul>
     *
     * @return Canal 同步服务状态描述
     * @see CanalSyncService#isRunning()
     */
    @GetMapping("/status")
    public String status() {
        boolean running = canalSyncService.isRunning();
        return running ? "Canal sync service is running" : "Canal sync service is stopped";
    }
}