package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.model.InplayEventDetails;
import com.asbresearch.collector.model.InplayEventDetails.InplayEventDetailsBuilder;
import com.asbresearch.collector.model.InplayEventExceptions;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.asbresearch.collector.derived.AnalyticEventsProvider.eventSql;
import static com.asbresearch.collector.util.CollectionUtils.partitionBasedOnSize;
import static com.asbresearch.common.BigQueryUtil.*;
import static java.time.Instant.ofEpochMilli;

@Component("BetfairSoccerInPlayEventDetails")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "inPlayEventDetails", havingValue = "on")
@EnableConfigurationProperties({CollectorProperties.class})
@DependsOn({"RemoveSecondHalfEndLessThan90Minutes"})
public class BetfairSoccerInPlayEventDetails {
    private final BigQueryService bigQueryService;
    private final String eventSql;

    @Autowired
    public BetfairSoccerInPlayEventDetails(BigQueryService bigQueryService,
                                           CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        eventSql = eventSql(collectorProperties);
    }

    @PostConstruct
    public void execute() {
        try {
            List<String> eventIds = eventIds();
            log.info("Returned {} eventIds to process {}", eventIds.size(), eventIds);
            if (!eventIds.isEmpty()) {
                insertEventDetails(eventIds);
            }
        } catch (RuntimeException | InterruptedException ex) {
            log.error("Error when processing data for SoccerInPlayEventDetails", ex);
        } finally {
            log.info("SoccerInPlayEventDetails completed");
        }
    }

    protected void waitForCompleteWriteToBigQuery(Map<String, Boolean> inplayEventDetails, Map<String, Boolean> inplayEventExceptions) {
        if (!inplayEventDetails.isEmpty()) {
            log.info("Total record to write to research.inplay_event_details = {}", inplayEventDetails.size());
            partitionBasedOnSize(inplayEventDetails.keySet(), 500).forEach(partitionIds -> {
                if (!partitionIds.isEmpty()) {
                    String idValues = partitionIds.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
                    String sql = String.format("SELECT count(*) as totalRows FROM `%s.%s` where id in(%s)", RESEARCH_DATASET, EVENT_DETAILS_TABLE, idValues);
                    waitForDbCompletion(sql, EVENT_DETAILS_TABLE, partitionIds.size());
                }
            });
            log.info("Complete writing to {}", EVENT_DETAILS_TABLE);
        }
        if (!inplayEventExceptions.isEmpty()) {
            log.info("Total record to write to research.inplay_event_exceptions = {}", inplayEventExceptions.size());
            partitionBasedOnSize(inplayEventExceptions.keySet(), 500).forEach(partitionIds -> {
                if (!partitionIds.isEmpty()) {
                    String idValues = partitionIds.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
                    String sql = String.format("SELECT count(*) as totalRows FROM `%s.%s` where id in(%s)", RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, idValues);
                    waitForDbCompletion(sql, INPLAY_EVENT_EXCEPTIONS_TABLE, partitionIds.size());
                }
            });
            log.info("Complete writing to {}", INPLAY_EVENT_EXCEPTIONS_TABLE);
        }
    }

    private void waitForDbCompletion(String sql, String table, int size) {
        log.info("sql={}", sql);
        int cycle = 100;
        while (cycle > 0) {
            log.info("Waiting for complete write to {}", table);
            try {
                List<Map<String, Optional<Object>>> countResult = bigQueryService.performQuery(sql);
                if (countResult.get(0).get("totalRows").isPresent()) {
                    long totalRows = Long.parseLong(countResult.get(0).get("totalRows").get().toString());
                    if (totalRows == size) {
                        break;
                    }
                    cycle--;
                    TimeUnit.SECONDS.sleep(30);
                }
            } catch (InterruptedException e) {
                log.error("Error trying to wait for complete writes to BQ", e);
                break;
            }
        }
    }

    private void insertEventDetails(List<String> events) {
        Map<String, Boolean> inPlayEventDetails = new HashMap<>();
        Map<String, Boolean> inPlayEventExceptions = new HashMap<>();
        try {
            log.info("Begin insertEventDetails");
            String eventIdsAsString = events.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
            String sql = String.format("SELECT eventId, unix_millis(updateTime) as updateTime, score, updateType FROM `%s.betfair_soccer_inplay` WHERE eventId in (%s) and updateType in ('KickOff', 'FirstHalfEnd', 'SecondHalfKickOff', 'SecondHalfEnd') order by eventId, matchTime ",
                    BETSTORE_DATASET, eventIdsAsString);
            log.info("Begin query sql={}", sql);
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
            log.info("End query");
            events.forEach(eventId -> {
                Instant kickOff = null;
                Instant firstHalfEnd = null;
                Instant secondHalfStart = null;
                Instant secondHalfEnd = null;
                String scoreFirstHalfEnd = null;
                String scoreSecondHalfEnd = null;
                List<Map<String, Optional<Object>>> eventRecords = rows.stream().filter(row -> row.get("eventId").get().toString().equals(eventId)).collect(Collectors.toList());
                Optional<Map<String, Optional<Object>>> kickOffRecordOpt = eventRecords.stream().filter(row -> row.get("updateType").get().toString().equals("KickOff")).findFirst();
                if (kickOffRecordOpt.isPresent()) {
                    Map<String, Optional<Object>> kickOffRecord = kickOffRecordOpt.get();
                    if(kickOffRecord.get("updateTime").isPresent()) {
                        kickOff = ofEpochMilli(Long.parseLong(kickOffRecord.get("updateTime").get().toString()));
                    }
                }
                if (kickOff == null) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("KickOff_IsEpoch").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                } else if (Instant.EPOCH.equals(kickOff)) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("KickOff_IsEpoch").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                }
                Optional<Map<String, Optional<Object>>> firstHalfEndOpt = eventRecords.stream().filter(row -> row.get("updateType").get().toString().equals("FirstHalfEnd")).findFirst();
                if (firstHalfEndOpt.isPresent()) {
                    Map<String, Optional<Object>> firstHalfEndRecord = firstHalfEndOpt.get();
                    if(firstHalfEndRecord.get("updateTime").isPresent()) {
                        firstHalfEnd = ofEpochMilli(Long.parseLong(firstHalfEndRecord.get("updateTime").get().toString()));
                    }
                    if(firstHalfEndRecord.get("score").isPresent()) {
                        scoreFirstHalfEnd = firstHalfEndRecord.get("score").get().toString();
                    }
                }
                if (firstHalfEnd == null) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("FirstHalfEnd_Missing").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                } else if (Instant.EPOCH.equals(firstHalfEnd)) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("FirstHalfEnd_IsEpoch").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                }
                Optional<Map<String, Optional<Object>>> secondHalfStartOpt = eventRecords.stream().filter(row -> row.get("updateType").get().toString().equals("SecondHalfKickOff")).findFirst();
                if (secondHalfStartOpt.isPresent()) {
                    Map<String, Optional<Object>> secondHalfStartRecord = secondHalfStartOpt.get();
                    if(secondHalfStartRecord.get("updateTime").isPresent()) {
                        secondHalfStart = ofEpochMilli(Long.parseLong(secondHalfStartRecord.get("updateTime").get().toString()));
                    }
                }
                if (secondHalfStart == null) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("SecondHalfKickOff_Missing").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                } else if (Instant.EPOCH.equals(secondHalfStart)) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("SecondHalfKickOff_IsEpoch").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                }
                Optional<Map<String, Optional<Object>>> secondHalfEndOpt = eventRecords.stream().filter(row -> row.get("updateType").get().toString().equals("SecondHalfEnd")).findFirst();
                if (secondHalfEndOpt.isPresent()) {
                    Map<String, Optional<Object>> secondHalfEndRecord = secondHalfEndOpt.get();
                    if(secondHalfEndRecord.get("updateTime").isPresent()) {
                        secondHalfEnd = ofEpochMilli(Long.parseLong(secondHalfEndRecord.get("updateTime").get().toString()));
                    }
                    if(secondHalfEndRecord.get("score").isPresent()) {
                        scoreSecondHalfEnd = secondHalfEndRecord.get("score").get().toString();
                    }
                }
                if (secondHalfEnd == null) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("SecondHalfEnd_Missing").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                } else if (Instant.EPOCH.equals(secondHalfEnd)) {
                    InplayEventExceptions row = InplayEventExceptions.builder().eventId(eventId).exceptionCode("SecondHalfEnd_IsEpoch").build();
                    bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_EVENT_EXCEPTIONS_TABLE, List.of(row.toString()));
                    inPlayEventExceptions.putIfAbsent(row.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                }
                if (kickOff != null && !kickOff.equals(Instant.EPOCH) && secondHalfEnd != null && !secondHalfEnd.equals(Instant.EPOCH)) {
                    InplayEventDetailsBuilder builder = InplayEventDetails.builder()
                            .source("Betfair")
                            .eventId(eventId)
                            .kickOffTime(kickOff)
                            .secondHalfEndTime(secondHalfEnd);
                    if (firstHalfEnd != null && !firstHalfEnd.equals(Instant.EPOCH)) {
                        builder = builder.firstHalfEndTime(firstHalfEnd);
                    }
                    if (secondHalfStart != null && !secondHalfStart.equals(Instant.EPOCH)) {
                        builder = builder.secondHalfStartTime(secondHalfStart);
                    }
                    if (scoreFirstHalfEnd != null) {
                        builder = builder.scoreFirstHalfEnd(scoreFirstHalfEnd);
                    }
                    if (scoreSecondHalfEnd != null) {
                        builder = builder.scoreSecondHalfEnd(scoreSecondHalfEnd);
                    }
                    InplayEventDetails eventDetails = builder.build();
                    log.debug("inserting into inplay_event_details row={}", eventDetails.toString());
                    bigQueryService.insertRows(RESEARCH_DATASET, EVENT_DETAILS_TABLE, List.of(eventDetails.toString()));
                    inPlayEventDetails.putIfAbsent(eventDetails.getBigQueryCreateRecord().getId(), Boolean.TRUE);
                }
            });
            log.info("End insertEventDetails");
        } catch (InterruptedException ex) {
            log.error("Error processing insertEventDetails data for {} events", events.size());
        } finally {
            waitForCompleteWriteToBigQuery(inPlayEventDetails, inPlayEventExceptions);
        }
    }

    private List<String> eventIds() throws InterruptedException {
        log.info("Begin eventIds");
        String sql = String.format("select distinct eventId from `betstore.betfair_soccer_inplay` where eventId in (%s) except distinct ( select eventId FROM `research.inplay_event_exceptions` union distinct select eventId from `research.inplay_event_details`)", eventSql);
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
        List<String> result = rows.stream()
                .map(row -> row.get("eventId").orElse("").toString())
                .filter(eventId -> !eventId.isBlank())
                .collect(Collectors.toList());
        log.info("End eventIds");
        return result;
    }
}