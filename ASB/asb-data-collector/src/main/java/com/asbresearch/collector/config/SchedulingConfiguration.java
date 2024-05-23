package com.asbresearch.collector.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.PreDestroy;

@Slf4j
@Configuration
public class SchedulingConfiguration implements SchedulingConfigurer {
    private final ThreadPoolTaskScheduler taskScheduler;

    public SchedulingConfiguration(CollectorProperties collectorProperties) {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setErrorHandler(t -> log.error("Exception in @Scheduled task.", t));
        taskScheduler.setThreadNamePrefix("@scheduled-");
        taskScheduler.setPoolSize(collectorProperties.getSchedulerPoolSize());
        taskScheduler.initialize();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler);
    }

    @PreDestroy
    public void shutDown() {
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }
}
