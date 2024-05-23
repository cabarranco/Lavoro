package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.asbresearch.collector.util.Constants.*;

@Service("AnalyticEventsProvider")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "analyticEventsProvider", havingValue = "on")
@DependsOn({"SofaScoreSoccerInPlayEventDetails"})
public class AnalyticEventsProvider {
    private final BigQueryService bigQueryService;
    private final Map<String, EventPeriod> events;

    public AnalyticEventsProvider(BigQueryService bigQueryService,
                                  CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        events = loadEvents(collectorProperties);
    }

    public String getInValues() {
        if (events.isEmpty()) {
            return "''";
        }
        return events.keySet().stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
    }

    private Map<String, EventPeriod> loadEvents(CollectorProperties collectorProperties) {
        log.info("Begin eventPeriods for all events");
        Map<String, EventPeriod> result = new ConcurrentHashMap<>();
        String sql = String.format("SELECT eventId, unix_millis(kickOffTime) as kickOffTime, unix_millis(secondHalfEndTime) as secondHalfEndTime  FROM `research.inplay_event_details` where kickOffTime is not null and secondHalfEndTime is not null and eventId in (%s)", eventSql(collectorProperties));
        try {
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
            for (Map<String, Optional<Object>> row : rows) {
                if (row.get(eventIdCol).isPresent()) {
                    String eventId = row.get(eventIdCol).get().toString();
                    Instant kickOffTime = Instant.ofEpochMilli(Long.parseLong(row.get(kickOffTimeCol).get().toString()));
                    Instant secondHalfEndTime = Instant.ofEpochMilli(Long.parseLong(row.get(secondHalfEndTimeCol).get().toString()));
                    result.put(eventId, new EventPeriod(kickOffTime, secondHalfEndTime));
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(String.format("Error occurred executing sql=%s", sql), e);
        }
        log.info("End eventPeriods returned {} rows", result.size());
        return result;
    }

    public Map<String, EventPeriod> eventPeriods() {
        return Collections.unmodifiableMap(events);
    }

    public static String eventSql(CollectorProperties collectorProperties) {
        String startDate = startDate(collectorProperties);
        String endDate = endDate(collectorProperties);
        return String.format("select distinct eventId from `betstore.betfair_market_catalogue` where startTime >= '%s 04:00:00 UTC' and  startTime < '%s 04:00:00 UTC'", startDate, endDate);
    }
}
