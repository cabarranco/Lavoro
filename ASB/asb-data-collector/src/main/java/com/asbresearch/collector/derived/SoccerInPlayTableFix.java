package com.asbresearch.collector.derived;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.bigquery.BigQueryService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.asbresearch.common.BigQueryUtil.RESEARCH_DATASET;
import static com.asbresearch.common.BigQueryUtil.csvValue;

@Component
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(prefix = "collector", name = "soccerInPlayTableFix", havingValue = "on")
public class SoccerInPlayTableFix {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.from(ZoneOffset.UTC));
    private final BigQueryService bigQueryService;

    @PostConstruct
    public void addUniqueIds() {
        List<String> insertStaging = new ArrayList<>();
        try {
            backUp();
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery("SELECT eventId, unix_millis(updateTime) as updateTime, matchTime, team, updateType , score FROM `betstore.betfair_soccer_inplay`");
            for (Map<String, Optional<Object>> row : rows) {
                List<String> stagingValues = new ArrayList<>();
                String uuid = BigQueryUtil.shortUUID();
                String eventId = row.get("eventId").isPresent() ? row.get("eventId").get().toString() : "";
                Long updateTime = row.get("updateTime").isPresent() ? Long.valueOf((String) row.get("updateTime").get()) : 0;
                Integer matchTime = row.get("matchTime").isPresent() ? Integer.valueOf((String) row.get("matchTime").get()) : 0;
                String team = row.get("team").isPresent() ? row.get("team").get().toString() : "";
                String updateType = row.get("updateType").isPresent() ? row.get("updateType").get().toString() : "";
                String score = row.get("score").isPresent() ? row.get("score").get().toString() : "";

                stagingValues.add(csvValue(uuid));
                stagingValues.add(csvValue(eventId));
                stagingValues.add(csvValue(Instant.ofEpochMilli(updateTime)));
                stagingValues.add(csvValue(matchTime));
                stagingValues.add(csvValue(team));
                stagingValues.add(csvValue(updateType));
                stagingValues.add(csvValue(score));
                insertStaging.add(stagingValues.stream().collect(Collectors.joining("|")));
            }

            bigQueryService.insertRows(RESEARCH_DATASET, "betfair_soccer_inplay_with_id_staging", insertStaging);
            long stagingCount = 0;
            while (stagingCount < insertStaging.size()) {
                List<Map<String, Optional<Object>>> stagingCountResult = bigQueryService.performQuery("select count(*) as stagingCount from `research.betfair_soccer_inplay_with_id_staging`");
                stagingCount = Long.valueOf((String) stagingCountResult.get(0).get("stagingCount").get());
                TimeUnit.MINUTES.sleep(1);
            }
            bigQueryService.performQuery("CREATE OR REPLACE TABLE `betstore.betfair_soccer_inplay` AS SELECT  * FROM `research.betfair_soccer_inplay_with_id_staging`");
        } catch (RuntimeException | InterruptedException ex) {
            log.error("Error in fixing betfair_soccer_inplay with unique ids", ex);
        }
    }

    private void backUp() throws InterruptedException {
        Instant today = Instant.now();
        String query = String.format("CREATE OR REPLACE TABLE `research.betfair_soccer_inplay_%s` AS SELECT  * FROM `betstore.betfair_soccer_inplay`", formatter.format(today));
        bigQueryService.performQuery(query);
    }
}
