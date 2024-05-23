package com.asbresearch.metrics.metrics.services;

import com.asbresearch.metrics.metrics.models.bigquery.BigQueryInsertLine;
import com.asbresearch.metrics.metrics.utils.ExecuteShellCommand;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@Service
public class BigQueryService {

    private static final Gson gson = new GsonBuilder().create();

    private final RestTemplate restTemplate;
    private final String gcloudToken;
    private final BigQuery bigQuery;

    @Value("${bigquery.endpoint}")
    private String bqUrl;

    @Autowired
    public BigQueryService(
            @Value("${credentials.path}") String credentialsPath,
            RestTemplateBuilder restTemplateBuilder
    ) throws IOException {
        this.restTemplate = restTemplateBuilder.build();

        ExecuteShellCommand com = new ExecuteShellCommand();
        this.gcloudToken = com.executeCommand("gcloud auth print-access-token");
//        this.gcloudToken = "ya29.a0AfH6SMBYP1E6xGSMD6I0J0U5ZLhPG2F0L36GNOcuYKbjuuCxLj34Hx1_EurSA6dlkcrTAAiKY1qK1xbqlJl7J9lzWiv9KUVTx7cPtvzdlbAXRkIc6fRfxlM8iUr_id9hS9mo-BdN2zlIWjczcWdMtFU8mr1YYKXUxc8PXKXkpE6S";

        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));

        this.bigQuery = BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .build().getService();
    }

    public boolean insertLine(String table, BigQueryInsertLine bigQueryInsertLine) {

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // set custom header
        headers.set("Authorization", "Bearer " + gcloudToken);
        headers.set("Content-Type", "application/json");

        String jsonRow = gson.toJson(bigQueryInsertLine);

        // build the request
        HttpEntity request = new HttpEntity<>(jsonRow, headers);


        String url = bqUrl + "asbanalytics/datasets/pulse_metrics/tables/" + table + "/insertAll?alt=json";
        ResponseEntity<String> response = this.restTemplate.postForEntity(
                url, request, String.class
        );

        return response.getStatusCode() == HttpStatus.OK;
    }

    public Iterable<FieldValueList> query(String query) throws InterruptedException {

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

        return bigQuery.query(queryConfig).iterateAll();
    }
}
