package com.asbresearch.collector.derived;

import com.asbresearch.betfair.inplay.model.SoccerInplayRecord;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.asbresearch.betfair.inplay.model.SoccerInplayRecord.TABLE;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(prefix = "collector", name = "fixMissingSecondHalfEnd", havingValue = "on")
public class FixMissingSecondHalfEnd {
    private final BigQueryService bigQueryService;

    @PostConstruct
    public void run() {
        try {
            List<String> toDelete = new ArrayList<>();
            String sql = "SELECT distinct eventId  FROM `research.inplay_event_exceptions` where exception_code = 'SecondHalfEnd_Missing'";
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
            Set<String> events = rows.stream().map(row -> row.get("eventId").get().toString()).collect(Collectors.toSet());
            log.info("Begin fixing missing SecondHalfEnd for {} events", events.size());
            String eventIdsAsString = events.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
            sql = String.format("select unix_millis(max(publishTime)) as publishTime, eventId from `betstore.betfair_historical_data` where eventId in (%s) and asbSelectionId in ('1', '2', '3') and status = 'OPEN' and inplay is true and DATE(publishTime) < current_date() group by eventId", eventIdsAsString);
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> publishTimeDbResult = bigQueryService.performQuery(sql);
            sql = String.format("SELECT unix_millis(updateTime) as updateTime, matchTime, score, eventId FROM `betstore.betfair_soccer_inplay` where eventId in (%s) order by eventId, matchTime desc", eventIdsAsString);
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> soccerDetailsDbResult = bigQueryService.performQuery(sql);
            if (!isEmpty(publishTimeDbResult)) {
                Map<String, Instant> secondHalfEndPerEvent = new HashMap<>();
                publishTimeDbResult.forEach(row -> {
                    String eventId = row.get("eventId").get().toString();
                    Instant secondHalfEndUpdateTime = Instant.ofEpochMilli(Long.valueOf(row.get("publishTime").get().toString()));
                    secondHalfEndPerEvent.putIfAbsent(eventId, secondHalfEndUpdateTime);
                });
                Map<String, SoccerDetail> soccerDetailsPerEvent = new HashMap<>();
                String currentEventId = "";
                if (!isEmpty(soccerDetailsDbResult)) {
                    for (Map<String, Optional<Object>> row : soccerDetailsDbResult) {
                        String eventId = row.get("eventId").get().toString();
                        if (!currentEventId.equals(eventId)) {
                            SoccerDetail soccerDetail = SoccerDetail.builder()
                                    .updateTime(Instant.ofEpochMilli(Long.valueOf(row.get("updateTime").get().toString())))
                                    .matchTime(Integer.valueOf(row.get("matchTime").get().toString())).score(row.get("score").get().toString()).build();
                            soccerDetailsPerEvent.putIfAbsent(eventId, soccerDetail);
                            currentEventId = eventId;
                        }
                    }
                }

                secondHalfEndPerEvent.forEach((eventId, secondHalfEndUpdateTime) -> {
                    SoccerDetail latestUpateDetail = soccerDetailsPerEvent.get(eventId);
                    if (latestUpateDetail != null) {
                        int secondHalfEndMatchTime;
                        if (latestUpateDetail.getUpdateTime().getEpochSecond() > 0) {
                            secondHalfEndMatchTime = latestUpateDetail.getMatchTime() + (int) Duration.between(latestUpateDetail.getUpdateTime(), secondHalfEndUpdateTime).toMinutes();
                        } else {
                            if (latestUpateDetail.getMatchTime() < 90) {
                                secondHalfEndMatchTime = 90;
                            } else {
                                secondHalfEndMatchTime = latestUpateDetail.getMatchTime() + 1;
                            }
                        }
                        SoccerInplayRecord secondHalfEndRecord = SoccerInplayRecord.builder()
                                .updateTime(secondHalfEndUpdateTime)
                                .eventId(Integer.valueOf(eventId))
                                .updateType("SecondHalfEnd")
                                .matchTime(secondHalfEndMatchTime)
                                .score(latestUpateDetail.getScore())
                                .build();
                        bigQueryService.insertRow(BETSTORE_DATASET, TABLE, secondHalfEndRecord.toString());
                        log.info("Added missing SecondHalfEnd eventId={} secondHalfEndRecord={}", eventId, secondHalfEndRecord);
                        toDelete.add(eventId);
                    }
                });
            }

            if (!isEmpty(toDelete)) {
                eventIdsAsString = toDelete.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
                sql = String.format("DELETE  FROM `research.inplay_event_exceptions` where eventId in (%s)", eventIdsAsString);
                log.info("sql={}", sql);
                bigQueryService.performQuery(sql);
            }
        } catch (RuntimeException | InterruptedException ex) {
            log.error("Error in fixing betfair_soccer_inplay with unique ids", ex);
        } finally {
            log.info("End fixing missing SecondHalfEnd");
        }
    }

    @Value
    @Builder
    private static class SoccerDetail {
        private Instant updateTime;
        private Integer matchTime;
        private String score;
    }
}
