package com.qixiaopi.canal.service;

/**
 * Canal 同步服务接口
 *
 * <p>功能说明：
 * <ul>
 *   <li>定义 Canal 数据同步服务的基本操作</li>
 *   <li>管理 Canal 连接的生命周期</li>
 *   <li>提供服务的启动、停止和状态查询</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * &#64;Autowired
 * private CanalSyncService canalSyncService;
 *
 * // 启动服务
 * canalSyncService.start();
 *
 * // 检查运行状态
 * boolean running = canalSyncService.isRunning();
 *
 * // 停止服务
 * canalSyncService.stop();
 * </pre>
 *
 * <p>实现类：
 * <ul>
 *   <li>{@link com.qixiaopi.canal.service.impl.CanalSyncServiceImpl}</li>
 * </ul>
 *
 * @author qixiaopi
 * @version 1.0.0
 * @see com.qixiaopi.canal.service.impl.CanalSyncServiceImpl
 */
public interface CanalSyncService {

    /**
     * 启动 Canal 同步服务
     *
     * <p>功能说明：
     * <ul>
     *   <li>连接到 Canal Server 集群（通过 ZooKeeper 发现）</li>
     *   <li>订阅配置的数据库和表</li>
     *   <li>启动数据同步线程</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>如果服务已经启动，此方法不会有任何效果</li>
     *   <li>启动后会创建多个线程，分别处理不同的 destination</li>
     *   <li>确保 Canal Server 和 ZooKeeper 集群已启动</li>
     * </ul>
     */
    void start();

    /**
     * 停止 Canal 同步服务
     *
     * <p>功能说明：
     * <ul>
     *   <li>断开与 Canal Server 的连接</li>
     *   <li>停止所有数据同步线程</li>
     *   <li>释放相关资源</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>如果服务未启动，此方法不会有任何效果</li>
     *   <li>停止后需要调用 start() 才能重新启动</li>
     *   <li>建议在应用关闭时调用此方法</li>
     * </ul>
     */
    void stop();

    /**
     * 检查服务是否正在运行
     *
     * @return true 表示服务正在运行，false 表示已停止
     */
    boolean isRunning();
}