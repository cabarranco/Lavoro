package com.asbresearch.collector;


import com.asbresearch.common.CredentialsUtility;
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Disabled
class BigQueryTest {
    String credentialLocation = "file:C:\\yomi\\src\\asb-data-collector\\data\\credentials\\asbresearch-dev-4dd04fe6ed02.json";
    String projectId = "asbresearch-dev";

    @Test
    void testPaging() throws Exception {
        String query = "SELECT * FROM `asbresearch-dev.research.event_prices_analytics` WHERE DATE(timestamp) = '2021-10-23' LIMIT 100";
        queryPagination(query);
    }

    private BigQuery makeBigQuery() throws IOException {
        BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(CredentialsUtility.googleCredentials(credentialLocation))
                .build();
        return bigQueryOptions.getService();
    }

    public void queryPagination(String query) {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            BigQuery bigQuery = makeBigQuery();

            TableId tableId = TableId.of("tmp", UUID.randomUUID().toString().replaceAll("-", ""));
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                    .setDestinationTable(tableId)
                    .build();
            bigQuery.query(queryConfig);
            TableResult results = bigQuery.listTableData(tableId, BigQuery.TableDataListOption.pageSize(20));
            // First Page
            log.info("TotalRows={}", results.getTotalRows());
            results.getSchema().getFields().forEach(field -> System.out.printf("%s, ", field.getName()));
            System.out.printf("\n");
            results.getValues()
                    .forEach(row -> {
                        row.forEach(fieldValue -> System.out.printf("%s, ", fieldValue.getValue().toString()));
                        System.out.printf("\n");
                    });
            if (results.hasNextPage()) {
                // Next Page
                results.getNextPage()
                        .getValues()
                        .forEach(row -> row.forEach(val -> System.out.printf("%s,\n", val.toString())));
            }

            if (results.hasNextPage()) {
                // Remaining Pages
                results.getNextPage()
                        .iterateAll()
                        .forEach(row -> row.forEach(val -> System.out.printf("%s,\n", val.toString())));
            }
            System.out.println("Query pagination performed successfully.");
        } catch (BigQueryException | InterruptedException | IOException e) {
            System.out.println("Query not performed \n" + e);
        }
    }
}
