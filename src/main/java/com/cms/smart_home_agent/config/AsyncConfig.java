package com.cms.smart_home_agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置：为 MQTT 消息处理创建专用线程池。
 * 使用 @EnableAsync 开启 Spring 的异步支持。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 定义 MQTT 消息处理的专用线程池。
     * 核心线程数应足够高，以应对突发的温湿度和状态更新。
     */
    @Bean(name = "mqttTaskExecutor")
    public Executor mqttTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：保持 5 个线程一直运行
        executor.setCorePoolSize(5);
        // 最大线程数：突发流量时，最多可扩展到 10 个线程
        executor.setMaxPoolSize(10);
        // 队列容量：最多允许 100 个任务等待
        executor.setQueueCapacity(100);
        // 线程名称前缀，方便日志跟踪
        executor.setThreadNamePrefix("MQTT-Processor-");
        // 拒绝策略：如果线程池和队列都满了，CallerRunsPolicy 会让提交线程（即 Paho 线程）自己执行任务。
        // 虽然会阻塞 Paho，但至少可以保证消息不丢失。更好的策略是等待 Broker 端的 Flow Control。
        // 但对于这种高优先级的 I/O 任务，我们不希望它被拒绝。
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        System.out.println("已配置 MQTT 专用线程池 'mqttTaskExecutor'");
        return executor;
    }
}
