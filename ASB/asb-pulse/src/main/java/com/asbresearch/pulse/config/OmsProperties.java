package com.asbresearch.pulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("oms")
public class OmsProperties {
    private int threads = 1;
    private boolean placeLiveOrder;
}
