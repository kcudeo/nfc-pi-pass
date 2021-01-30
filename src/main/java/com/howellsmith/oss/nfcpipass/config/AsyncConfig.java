package com.howellsmith.oss.nfcpipass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Create a configurable thread pool task executor and make it the default for the application @async methods.
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    private final int asyncCorePoolSize;
    private final int asyncMaxPoolSize;
    private final String asyncThreadNamePrefix;

    public AsyncConfig(
            @Value("${async.core_pool_size:10}") int asyncCorePoolSize,
            @Value("${async.max_pool_size:10}") int asyncMaxPoolSize,
            @Value("${async.thread_name_prefix:pipass_async_}") String asyncThreadNamePrefix) {
        this.asyncCorePoolSize = asyncCorePoolSize;
        this.asyncMaxPoolSize = asyncMaxPoolSize;
        this.asyncThreadNamePrefix = asyncThreadNamePrefix;
    }

    @Bean("ThreadPoolTaskExecutor")
    public Executor taskExecutor() {

        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncCorePoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix(asyncThreadNamePrefix);
        executor.initialize();

        return executor;
    }
}
