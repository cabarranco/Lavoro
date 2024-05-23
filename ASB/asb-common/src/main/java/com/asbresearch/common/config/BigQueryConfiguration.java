package com.asbresearch.common.config;

import com.asbresearch.common.CredentialsUtility;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BigQueryProperties.class)
public class BigQueryConfiguration {

    private final BigQueryProperties bigQueryProperties;

    public BigQueryConfiguration(BigQueryProperties bigQueryProperties) {
        this.bigQueryProperties = bigQueryProperties;
    }

    @Bean
    @Qualifier("primary")
    public BigQuery bigQuery() throws IOException {
        BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder()
                .setProjectId(bigQueryProperties.getProjectId())
                .setCredentials(CredentialsUtility.googleCredentials(bigQueryProperties.getCredentials().getLocation()))
                .build();
        return bigQueryOptions.getService();
    }
}
