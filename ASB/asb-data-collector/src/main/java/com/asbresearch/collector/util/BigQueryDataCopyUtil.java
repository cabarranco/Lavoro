package com.asbresearch.collector.util;

import com.asbresearch.common.bigquery.BigQueryService;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.BigQueryUtil.csvValue;
import static com.asbresearch.common.BigQueryUtil.shortUUID;

@Slf4j
public class BigQueryDataCopyUtil {
    public static void main(String[] args) throws IOException, InterruptedException {
        BigQueryService asbanalytics = createBigQueryService("asbanalytics", "C:\\AsbResearch\\asb-data-collector\\data\\credentials\\credentials.json");
        BigQueryService asbresearchProd = createBigQueryService("asbresearch-prod", "C:\\AsbResearch\\asb-data-collector\\data\\credentials\\asbresearch-prod-910a3d64fcc9.json");
        copyHistoricalData(asbanalytics, asbresearchProd);
        TimeUnit.MINUTES.sleep(5);
    }

    private static void copyHistoricalData(BigQueryService source, BigQueryService destination) throws InterruptedException {
        log.info("Begin copyHistoricalData");
        try {
            Instant start = Instant.now();
            String sql = "select  eventId, marketId, asbSelectionId, selectionId, status, inplay, totalMatched, backPrice, backSize, layPrice, laySize, unix_millis(publishTime) as publishTime from `research.betfair_historical_data_copy` where DATE(publishTime)  between '2020-06-14' and '2020-06-15' ";
            List<Map<String, Optional<Object>>> rows = source.performQuery(sql).stream().map(row -> {
                Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get("publishTime").get().toString()));
                row.put("publishTime", Optional.of(startTime));
                return row;
            }).collect(Collectors.toList());
            log.info("Read {} rows in {}s", rows.size(), TimeUnit.MILLISECONDS.toSeconds(Duration.between(start, Instant.now()).toMillis()));

            start = Instant.now();
            rows.forEach(row -> {
                String entry = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                        csvValue(row.get("eventId").orElse("")),
                        csvValue(row.get("marketId").orElse("")),
                        csvValue(row.get("asbSelectionId").orElse("")),
                        csvValue(row.get("selectionId").orElse("")),
                        csvValue(row.get("status").orElse("")),
                        csvValue(row.get("inplay").orElse("")),
                        csvValue(row.get("totalMatched").orElse("")),
                        csvValue(row.get("backPrice").orElse("")),
                        csvValue(row.get("backSize").orElse("")),
                        csvValue(row.get("layPrice").orElse("")),
                        csvValue(row.get("laySize").orElse("")),
                        csvValue(row.get("publishTime").orElse("")));
                destination.insertRows(BETSTORE_DATASET, "betfair_historical_data", Arrays.asList(entry));
            });
            log.info("Write data took {}s", TimeUnit.MILLISECONDS.toSeconds(Duration.between(start, Instant.now()).toMillis()));
        } finally {
            log.info("End copyHistoricalData");
        }
    }

    private static void copySoccerInPlay(BigQueryService asbanalytics, BigQueryService asbanalyticsb) throws InterruptedException {
        String sql = "select id, eventId, unix_millis(updateTime) as updateTime, matchTime, team, updateType, score from `betstore.betfair_soccer_inplay` where eventId in (\n" +
                "SELECT distinct eventId FROM `betstore.betfair_market_catalogue` where startTime < '2020-12-02 04:00:00 UTC' and asbSelectionId = '1')";
        List<Map<String, Optional<Object>>> rows = asbanalyticsb.performQuery(sql).stream().map(row -> {
            Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get("updateTime").get().toString()));
            row.put("updateTime", Optional.of(startTime));
            return row;
        }).collect(Collectors.toList());

        rows.forEach(row -> {
            String entry = String.format("%s|%s|%s|%s|%s|%s|%s",
                    csvValue(row.get("id").orElse("")),
                    csvValue(row.get("eventId").orElse("")),
                    csvValue(row.get("updateTime").orElse("")),
                    csvValue(row.get("matchTime").orElse("")),
                    csvValue(row.get("team").orElse("")),
                    csvValue(row.get("updateType").orElse("")),
                    csvValue(row.get("score").orElse("")));
            asbanalytics.insertRows(BETSTORE_DATASET, "betfair_soccer_inplay", Arrays.asList(entry));
        });
    }

    private static void copyMarketCatalogue(BigQueryService asbanalytics, BigQueryService asbanalyticsb) throws InterruptedException {
        String sql = "select distinct unix_millis(startTime) as startTime, competition, eventName, eventId, marketName, marketId, runnerName, selectionId, asbSelectionId, from `betstore.BETFAIR_MARKET_CATALOGUE_TABLE` where eventId in \n" +
                "(\n" +
                "  SELECT distinct eventId FROM `betstore.betfair_market_catalogue` where startTime < '2020-12-02 04:00:00 UTC' and asbSelectionId = '1' \n" +
                ")";
        List<Map<String, Optional<Object>>> rows = asbanalyticsb.performQuery(sql).stream().map(row -> {
            Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get("startTime").get().toString()));
            row.put("startTime", Optional.of(startTime));
            return row;
        }).collect(Collectors.toList());

        rows.forEach(row -> {
            String entry = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    csvValue(row.get("startTime").orElse("")),
                    csvValue(row.get("competition").orElse("")),
                    csvValue(row.get("eventName").orElse("")),
                    csvValue(row.get("eventId").orElse("")),
                    csvValue(row.get("marketName").orElse("")),
                    csvValue(row.get("marketId").orElse("")),
                    csvValue(row.get("runnerName").orElse("")),
                    csvValue(row.get("selectionId").orElse("")),
                    csvValue(row.get("asbSelectionId").orElse("")),
                    csvValue(shortUUID()));
            asbanalytics.insertRows(BETSTORE_DATASET, "betfair_market_catalogue", Arrays.asList(entry));
        });
    }

    private static BigQueryService createBigQueryService(String projectId, String credentialLocation) throws IOException {
//        BigQueryProperties bigQueryProperties = new BigQueryProperties();
//        bigQueryProperties.getCredentials().setLocation(credentialLocation);
//        EmailProperties emailProperties = new EmailProperties();
//        EmailNotifier emailNotifier = new EmailNotifier(new JavaMailSenderImpl(), new ObjectMapper(), emailProperties);
//        BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder()
//                .setProjectId(projectId)
//                .setCredentials(CredentialsUtility.googleCredentials(bigQueryProperties.getCredentials().getLocation()))
//                .build();
//        BigQueryService bigQueryService = new BigQueryService(bigQueryOptions.getService(), bigQueryProperties, emailProperties, emailNotifier);
//        return bigQueryService;
        return null;
    }
}
