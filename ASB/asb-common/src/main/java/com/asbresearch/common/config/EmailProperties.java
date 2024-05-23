package com.asbresearch.common.config;

import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("email")
public class EmailProperties {
    private List<String> to = Arrays.asList("pulsealerts_prod@asbresearch.com");
    private String subject = "Pulse: Notification";
    private boolean notification;
}
