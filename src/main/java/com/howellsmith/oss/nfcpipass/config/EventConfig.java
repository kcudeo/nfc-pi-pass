package com.howellsmith.oss.nfcpipass.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

import java.util.concurrent.Executor;

/**
 * Eventing configuration that enables async operations.
 */
@Configuration
public class EventConfig {

    private final Executor taskExecutor;

    public EventConfig(@Qualifier("ThreadPoolTaskExecutor") Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Bean
    public ApplicationEventMulticaster multicaster() {
        var multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(taskExecutor);
        return multicaster;
    }
}
