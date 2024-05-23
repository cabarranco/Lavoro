package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.asbresearch.collector.derived.AnalyticEventsProvider.eventSql;
import static com.asbresearch.collector.util.CollectionUtils.isEmpty;
import static com.asbresearch.collector.util.CollectionUtils.partitionBasedOnSize;
import static com.asbresearch.collector.util.Constants.endDate;
import static com.asbresearch.collector.util.Constants.startDate;
import static com.asbresearch.common.BigQueryUtil.*;

@Component("InPlayUpdateTimeEpochFixer")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "inPlayUpdateTimeEpochFixer", havingValue = "on")
public class InPlayUpdateTimeEpochFixer {
    private static final String SYNTHETIC_KICK_OFF_TIME_SQL = "SELECT  '%s' as eventId, " +
            "  (select unix_millis(min(publishTime)) FROM `betstore.betfair_historical_data` WHERE eventId = '%s' and inplay is true and publishTime between '%s 04:00:00 UTC' and '%s 04:00:00 UTC') inPlayStart, " +
            "  (select unix_millis(max(publishTime)) FROM `betstore.betfair_historical_data` WHERE eventId = '%s' and inplay is false and publishTime between '%s 04:00:00 UTC' and '%s 04:00:00 UTC') preLiveEnd";

    private static final String UPDATE_TIME_SQL = "MERGE `betstore.betfair_soccer_inplay` I USING `research.betfair_soccer_inplay_1970_staging` S " +
            "ON I.id = S.id AND S.createCorrelationId = '%s' " +
            "WHEN MATCHED THEN  UPDATE SET I.updateTime = S.updateTime ";

    private static final String STAGING_COUNT_SQL = "select count(*) as stagingCount from `research.betfair_soccer_inplay_1970_staging` where createCorrelationId = '%s'";

    private static final String KICKOFF_UPDATE_TIME_SQL = "select distinct eventId, unix_millis(updateTime) as updateTime  from `betstore.betfair_soccer_inplay` where unix_millis(updateTime) > 0 and updateType = 'KickOff' and eventId in (%s)";

    private final BigQueryService bigQueryService;
    private final String eventSql;
    private final String startDate;
    private final String endDate;

    @Autowired
    public InPlayUpdateTimeEpochFixer(BigQueryService bigQueryService, CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        eventSql = eventSql(collectorProperties);
        this.startDate = startDate(collectorProperties);
        this.endDate = endDate(collectorProperties);
        cleanEpochUpdateTimes();
        log.info("InPlayUpdateTimeEpochFixer completed");
    }

    public void cleanEpochUpdateTimes() {
        try {
            Map<String, Instant> syntheticKickOffTimes = syntheticKickOffForEpochKickOff();
            Map<String, Instant> kickOffTimes = kickOffTimes();
            if (!isEmpty(syntheticKickOffTimes) || !isEmpty(kickOffTimes)) {
                Map<String, Instant> allKickOffTimes = Stream.of(syntheticKickOffTimes, kickOffTimes)
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
                doCleanUp(allKickOffTimes);
            }
        } catch (RuntimeException | InterruptedException ex) {
            log.error("Error when cleaning update time", ex);
        }
    }

    private Map<String, Instant> kickOffTimes() {
        Map<String, Instant> result = new HashMap<>();
        log.info("Begin kickOffTimes");
        String sql = String.format(KICKOFF_UPDATE_TIME_SQL, eventSql);
        log.info("Running sql={}", sql);
        try {
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
            log.info("End sql returned {} rows", rows.size());
            for (Map<String, Optional<Object>> row : rows) {
                Optional<Object> eventId = row.get("eventId");
                Optional<Object> updateTime = row.get("updateTime");
                if (eventId.isPresent() && updateTime.isPresent()) {
                    long updateTimeValue = Long.parseLong((String) updateTime.get());
                    result.putIfAbsent((String) (eventId.get()), Instant.ofEpochMilli(updateTimeValue));
                }
            }
        } catch (InterruptedException e) {
            log.error("Error processing sql={}", sql);
        }
        log.info("End kickOffTimes for {} eventIds ", result.size());
        return result;
    }

    private void doCleanUp(Map<String, Instant> kickOffTimes) throws InterruptedException {
        log.info("Begin doCleanUp");
        try {
            Instant currentTime = Instant.now();
            String query = String.format("SELECT id, eventId, matchTime,  updateType FROM `betstore.betfair_soccer_inplay` where unix_millis(updateTime) = 0 and eventId  in (%s) order by eventId, matchTime", eventSql);
            log.info("Begin sql={}", query);
            List<Map<String, Optional<Object>>> rowsToUpdate = bigQueryService.performQuery(query);
            Set<String> returnedEvents = new HashSet<>();
            rowsToUpdate.forEach(row -> returnedEvents.add(row.get("eventId").get().toString()));
            log.info("End with {} events to fix events={}", returnedEvents.size(), returnedEvents);

            String uuid = BigQueryUtil.shortUUID();
            List<String> insertStaging = new ArrayList<>();
            Set<String> eventsToUpdate = new HashSet<>();
            for (String eventId : kickOffTimes.keySet()) {
                List<Map<String, Optional<Object>>> perEvent = rowsToUpdate.stream().filter(row -> row.get("eventId").get().equals(eventId)).collect(Collectors.toList());
                if (!isEmpty(perEvent)) {
                    eventsToUpdate.add(eventId);
                    boolean isSecondHalfStartFound = false;
                    for (Map<String, Optional<Object>> updateDetail : perEvent) {
                        List<String> stagingValues = new ArrayList<>();
                        if (updateDetail.get("updateType").get().equals("SecondHalfKickOff")) {
                            isSecondHalfStartFound = true;
                        }
                        long matchTimeValue = Long.parseLong((String) updateDetail.get("matchTime").get());
                        stagingValues.add(updateDetail.get("id").get().toString());
                        if (isSecondHalfStartFound) {
                            stagingValues.add(csvValue(kickOffTimes.get(eventId).plus(matchTimeValue + 15, ChronoUnit.MINUTES)));
                        } else {
                            stagingValues.add(csvValue(kickOffTimes.get(eventId).plus(matchTimeValue, ChronoUnit.MINUTES)));
                        }
                        stagingValues.add(csvValue(currentTime));
                        stagingValues.add(csvValue(uuid));
                        insertStaging.add(String.join("|", stagingValues));
                    }
                }
            }
            log.info("End with {} events fixed events={}", eventsToUpdate.size(), eventsToUpdate);
            if (!insertStaging.isEmpty()) {
                bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_STAGING_TABLE, insertStaging);
                long stagingCount = 0;
                while (stagingCount < insertStaging.size()) {
                    String sql = String.format(STAGING_COUNT_SQL, uuid);
                    List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
                    stagingCount = Long.parseLong((String) rows.get(0).get("stagingCount").get());
                    TimeUnit.SECONDS.sleep(1);
                }
                String mergeSql = String.format(UPDATE_TIME_SQL, uuid);
                log.info("Begin merge sql={}", mergeSql);
                bigQueryService.performQuery(mergeSql);
                log.info("Begin merge successfully");
                bigQueryService.performQuery(String.format("DELETE from `%s.%s` where createCorrelationId = '%s'", RESEARCH_DATASET, INPLAY_STAGING_TABLE, uuid));
                log.info("betfair_soccer_inplay_1970_staging cleaned successfully");
            }
        } finally {
            log.info("End doCleanUp");
        }
    }

    private Map<String, Instant> syntheticKickOffForEpochKickOff() throws InterruptedException {
        List<String> eventIds = eventsWithEpochKickOff();
        Map<String, Instant> result = new HashMap<>();
        if (!isEmpty(eventIds)) {
            log.info("Begin synthetic kickOffTimes for {} eventIds ", eventIds.size());
            for (List<String> partitionedEventId : partitionBasedOnSize(eventIds, 1000)) {
                String sql = createWithSql(partitionedEventId.stream().map(eventId -> String.format(SYNTHETIC_KICK_OFF_TIME_SQL, eventId, eventId, startDate, endDate, eventId, startDate, endDate)).collect(Collectors.joining(" UNION ALL ")));
                log.info("Running sql={}", sql);
                try {
                    List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
                    log.info("End sql returned {} rows", rows.size());
                    for (Map<String, Optional<Object>> row : rows) {
                        Optional<Object> eventId = row.get("eventId");
                        Optional<Object> inPlayStart = row.get("inPlayStart");
                        Optional<Object> preLiveEnd = row.get("preLiveEnd");
                        if (eventId.isPresent() && inPlayStart.isPresent() && preLiveEnd.isPresent()) {
                            Long inPlayStartValue = Long.valueOf((String) inPlayStart.get());
                            Long preLiveEndValue = Long.valueOf((String) preLiveEnd.get());
                            long avg = (inPlayStartValue + preLiveEndValue) / 2;
                            result.putIfAbsent((String) (eventId.get()), Instant.ofEpochMilli(avg));
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("Error processing sql={}", sql);
                }
            }
            log.info("End synthetic kickOffTimes for {} eventIds ", result.size());
        }
        return result;
    }

    private String createWithSql(String selectQuery) {
        return String.format("WITH kickOff_synthetic AS (%s) SELECT *  FROM `kickOff_synthetic` where inPlayStart is not null and preLiveEnd is not null", selectQuery);
    }

    private List<String> eventsWithEpochKickOff() throws InterruptedException {
        log.info("Begin eventsWithEpochUpdateTime");
        String sql = String.format("select distinct eventId from `betstore.betfair_soccer_inplay` where unix_millis(updateTime) = 0 and updateType = 'KickOff' and eventId in (%s)", eventSql);
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
        List<String> result = rows.stream().map(row -> String.valueOf(row.get("eventId").get())).collect(Collectors.toList());
        log.info("End eventsWithEpochUpdateTime with {} rows eventIds={}", result.size(), result);
        return result;
    }
}
