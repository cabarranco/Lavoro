package com.asbresearch.betfair.inplay;

import com.asbresearch.common.CredentialsUtility;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQueryOptions;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Slf4j
@Ignore
public class CompetitionLiquidityReportTest {

    @Test
    public void test() throws Exception {
        BigQueryService bgs = createBigQueryService();
        String events = "SELECT startTime, eventId FROM `asbanalytics.betstore.betfair_market_catalogue` where asbSelectionId = '1' and competition = \"%s\" and eventId in (SELECT distinct eventId FROM `asbanalytics.betstore.betfair_historical_data`) order by startTime desc";
        String liquity = "SELECT totalMatched FROM `asbanalytics.betstore.betfair_historical_data` where eventId = '%s' and UNIX_SECONDS(publishTime) < %s  and asbSelectionId = '1' order by publishTime desc LIMIT 1";
        List<String> competitions = competitons(bgs);
        for (String competition : competitions) {
            log.info("competition={}", competition);
            List<String> liquidityRows = new ArrayList<>();
            Map<String, List<Double>> liquidityMappings = new HashMap<>();
            String eventSql = String.format(events, competition);
            List<Map<String, Optional<Object>>> eventsResult = bgs.performQuery(eventSql);
            for (Map<String, Optional<Object>> eventRow : eventsResult) {
                Optional<Object> startTime = eventRow.get("startTime");
                Optional<Object> eventId = eventRow.get("eventId");
                if (startTime.isPresent() && eventId.isPresent()) {
                    Instant instant = Instant.ofEpochSecond(Double.valueOf(startTime.get().toString()).longValue());
                    String date = instant.toString().split("T")[0];
                    String liquitySql = String.format(liquity, eventId.get().toString(), instant.getEpochSecond());
                    List<Map<String, Optional<Object>>> liquidityResult = bgs.performQuery(liquitySql);
                    if (!liquidityResult.isEmpty()) {
                        Optional<Object> totalMatched = liquidityResult.get(0).get("totalMatched");
                        if (totalMatched.isPresent()) {
                            liquidityMappings.putIfAbsent(date, new ArrayList<>());
                            List<Double> eventMatchedPerDate = liquidityMappings.get(date);
                            eventMatchedPerDate.add(Double.valueOf(totalMatched.get().toString()));
                        }
                    }
                }
            }
            log.info("competition={} dates={}", competition, liquidityMappings.size());
            for (Map.Entry<String, List<Double>> entry : liquidityMappings.entrySet()) {
                liquidityRows.add(String.format("%s|%s|%s|%s|%s|1", entry.getKey(), competition, avg(entry.getValue()), min(entry.getValue()), max(entry.getValue())));
            }
            bgs.insertRows("asb_research", "competition_liquidity", liquidityRows);
        }

        TimeUnit.MINUTES.sleep(10);
        bgs.shutDown();
    }

    private static double avg(List<Double> liquidity) {
        if (liquidity.isEmpty()) {
            return 0.0;
        }
        return liquidity.stream().mapToDouble(value -> value).average().getAsDouble();
    }

    private static double min(List<Double> liquidity) {
        if (liquidity.isEmpty()) {
            return 0.0;
        }
        return liquidity.stream().mapToDouble(value -> value).min().getAsDouble();
    }

    private static double max(List<Double> liquidity) {
        if (liquidity.isEmpty()) {
            return 0.0;
        }
        return liquidity.stream().mapToDouble(value -> value).max().getAsDouble();
    }

    private static List<String> competitons(BigQueryService bgs) throws InterruptedException {
        List<String> result = new ArrayList<>();
        String sql = "SELECT distinct competition FROM `asbanalytics.betstore.betfair_market_catalogue` where asbSelectionId = '1'";
        List<Map<String, Optional<Object>>> competitionResults = bgs.performQuery(sql);
        for (Map<String, Optional<Object>> row : competitionResults) {
            Optional<Object> competition = row.get("competition");
            if (competition.isPresent()) {
                result.add(competition.get().toString());
            }
        }
        return result;
    }

    private static BigQueryService createBigQueryService() throws IOException {
        BigQueryProperties bigQueryProperties = new BigQueryProperties();
        EmailProperties emailProperties = new EmailProperties();
        EmailNotifier emailNotifier = new EmailNotifier(new JavaMailSenderImpl(), new ObjectMapper(), emailProperties);
        BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder()
                .setProjectId("asbanalytics")
                .setCredentials(CredentialsUtility.googleCredentials(bigQueryProperties.getCredentials().getLocation()))
                .build();
        BigQueryService bigQueryService = new BigQueryService(bigQueryOptions.getService(), bigQueryProperties, emailProperties, emailNotifier);
        return bigQueryService;
    }
}
