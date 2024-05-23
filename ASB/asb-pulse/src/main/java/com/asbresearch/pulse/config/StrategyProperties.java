package com.asbresearch.pulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("strategy")
public class StrategyProperties {
    private int threads = 1;
    private int tradingHours = 24;
    private int maxTrade = 500;
}
