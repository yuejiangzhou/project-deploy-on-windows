package com.company.deploy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${package.max-concurrent-tasks:2}")
    private int maxConcurrentTasks;

    @Bean("packageTaskExecutor")
    public Executor packageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrentTasks);
        executor.setMaxPoolSize(maxConcurrentTasks * 2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("package-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}