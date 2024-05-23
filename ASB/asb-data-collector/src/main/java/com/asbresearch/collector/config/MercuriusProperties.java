package com.asbresearch.collector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mercurius")
public class MercuriusProperties {
    private String tradePath;
}
