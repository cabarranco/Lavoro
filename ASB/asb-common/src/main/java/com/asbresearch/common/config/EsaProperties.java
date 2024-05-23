package com.asbresearch.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("esa")
public class EsaProperties {
    private String host;
    private int maxConnections = 1;
    private int port = 443;
    private int maxMarketIdsPerConn = 1000;
    private int connectionErrorNotificationInMin = 60;
}
