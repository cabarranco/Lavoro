package com.asb.analytics.bigquery;

import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.logs.Logger;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

public class BigQueryServices {

    private final String dataSets;

    public static BigQueryServices oddsSizes() {
        return new BigQueryServices(BigQueryDataSets.BETSTORE);
    }

    public static BigQueryServices pulseReporting() {
        return new BigQueryServices(BigQueryDataSets.PULSE_REPORTING);
    }

    public static BigQueryServices staticTables() {
        return new BigQueryServices(BigQueryDataSets.STATIC_TABLES);
    }

    private BigQueryServices(String dataSets) {
        this.dataSets = dataSets;
    }

    public void insertBigQuery(Gson gson, List<Row> values, String token, String table) {
        String body = gson.toJson(new BigQueryInsertLine(values));

        Logger.log().info("BigQuery add to: " + dataSets + "/" + table);
        Logger.log().info("Rows numbers: " + values.size() );

        try {
            SimpleResponse response = HttpConnector
                    .connect("https://content-bigquery.googleapis.com/bigquery/v2/projects/asbanalytics/datasets/"+dataSets+"/tables/"+table+"/insertAll?alt=json")
                    .timeout(5000)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .method(HttpConnector.POST)
                    .execute();

            Logger.log().info("BigQuery insert response code: " + response.getCode());

            if (response.getCode() != 200) {
                Logger.log().info("BigQuery insert response body: " + response.getBody());
            }

        } catch (Exception e) {
            Logger.log().error("BIG QUERY ERROR");
            Logger.log().error(e.getMessage());
        }

    }
}