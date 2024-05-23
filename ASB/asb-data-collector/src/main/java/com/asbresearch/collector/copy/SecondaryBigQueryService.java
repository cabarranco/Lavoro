package com.asbresearch.collector.copy;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.google.cloud.bigquery.BigQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableConfigurationProperties({BigQueryProperties.class, EmailProperties.class})
@Slf4j
@ConditionalOnProperty(prefix = "copy", name = "secondaryBigQueryService", havingValue = "on")
public class SecondaryBigQueryService {

    private BigQueryService bigQueryService;

    public SecondaryBigQueryService(@Qualifier("secondary") BigQuery bigQuery,
                                    BigQueryProperties bigQueryProperties,
                                    EmailProperties emailProperties,
                                    EmailNotifier emailNotifier) {

//        bigQueryService = new BigQueryService(bigQuery, bigQueryProperties, emailProperties, emailNotifier);
    }

    public void insertRows(String datasetName, String tableName, List<String> csvRows) {
        bigQueryService.insertRows(datasetName, tableName, csvRows);
    }

    @PreDestroy
    public void shutDown() {
        bigQueryService.shutDown();
    }

    public List<Map<String, Optional<Object>>> performQuery(String query) throws InterruptedException {
        return bigQueryService.performQuery(query);
    }
}
