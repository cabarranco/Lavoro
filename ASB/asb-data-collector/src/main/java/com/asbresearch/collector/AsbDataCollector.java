package com.asbresearch.collector;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({BigQueryProperties.class, EmailProperties.class})
@ComponentScan(basePackageClasses = {AsbDataCollector.class, BigQueryUtil.class, BetfairInPlayService.class})
@EnableScheduling
@EnableAsync
@Slf4j
public class AsbDataCollector {
    public static void main(String[] args) {
        SpringApplication.run(AsbDataCollector.class, args);
    }
}