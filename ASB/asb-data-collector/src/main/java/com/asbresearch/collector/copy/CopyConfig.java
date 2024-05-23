package com.asbresearch.collector.copy;

import com.asbresearch.common.CredentialsUtility;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BigQueryProperties.class, EmailProperties.class})
public class CopyConfig {

    @Autowired
    private BigQueryProperties bigQueryProperties;

    @Bean
    @Qualifier("secondary")
    public BigQuery secondaryBigQuery() {

        BigQueryOptions bigQueryOptions;
        try {
            bigQueryOptions = BigQueryOptions.newBuilder()
                    .setProjectId(bigQueryProperties.getSecondaryProjectId())
                    .setCredentials(CredentialsUtility.googleCredentials(bigQueryProperties.getSecondaryCredentials().getLocation()))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error getting big query credentials from %s", bigQueryProperties.getCredentials().getLocation()), e);
        }
        return bigQueryOptions.getService();
    }

}
