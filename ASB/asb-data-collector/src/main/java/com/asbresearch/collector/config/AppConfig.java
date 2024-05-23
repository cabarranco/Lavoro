package com.asbresearch.collector.config;

import com.asbresearch.betfair.esa.cache.util.MarketSnaps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public MarketSnaps marketSnaps() {
        return new MarketSnaps();
    }
}
