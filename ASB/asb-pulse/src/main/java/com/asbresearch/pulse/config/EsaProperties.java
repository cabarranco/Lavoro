package com.asbresearch.pulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("esa")
public class EsaProperties {
    private String host;
    private int maxConnections = 4;
    private int port = 443;
}
