package com.solis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

//线程池配置，应对高并发场景下的性能问题
@Configuration
public class ThreadPoolConfig {
    /**
     *配置Spring封装的线程池
     * 用于处理@Async异步任务，自定义异步任务的执行
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        //阻塞队列容量
        executor.setQueueCapacity(200);
        //非核心线程存活时间
        executor.setKeepAliveSeconds(30);
        //线程名称前缀，用于调试和监控
        executor.setThreadNamePrefix("NoteExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
