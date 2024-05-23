package com.asbresearch.collector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("copy")
public class CopyProperties {
    private String historicalStartDate;
    private String historicalEndDate;
    private String csvUploadDir;
    private String datasetName;
    private String tableName;
}
