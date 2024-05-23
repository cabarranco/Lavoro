package com.asbresearch.pulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app")
public class AppProperties {
    private String cronExpression = "0 0 3 * * *";
    private int maxOpportunityBets = 100;
    private String logDirectory;
    private String dataDirectory;
    private int opportunityQueueCapacity = 1000;
    private boolean logAudit;
    private boolean simulationMode;
    private String node;
}
