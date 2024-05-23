package com.asbresearch.collector.derived;

import com.asbresearch.collector.betfair.EventTransactionDb;
import com.asbresearch.collector.betfair.model.BetfairHistoricalRecord;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.model.EventInplayFeature;
import com.asbresearch.collector.model.EventPreLiveFeature;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.asbresearch.collector.derived.AnalyticEventsProvider.eventSql;
import static com.asbresearch.collector.util.CollectionUtils.isEmpty;
import static com.asbresearch.collector.util.Constants.*;
import static com.asbresearch.common.BigQueryUtil.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.subtract;

@Component
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "eventFeaturesCalc", havingValue = "on")
@DependsOn({"SofaScoreSoccerInPlayEventDetails"})
public class EventFeaturesCalc {
    private static final List<String> ASB_SELECTION_IDS = List.of("1", "300", "2051", "2151", "2251", "2231", "41100");
    private static final List<String> FEATURES_TABLES = List.of("research.event_inplay_features", "research.event_prelive_features");
    private final BigQueryService bigQueryService;
    private final String startDate;
    private final String endDate;
    private final AnalyticEventsProvider analyticEventsProvider;
    private final EventTransactionDb eventTransactionDb;
    private final List<Map<String, Optional<Object>>> soccerInPlayData;
    private final Map<String, Map<String, Instant>> incompleteFeatures = new ConcurrentHashMap<>();
    private final CollectorProperties collectorProperties;

    public EventFeaturesCalc(BigQueryService bigQueryService,
                             CollectorProperties collectorProperties,
                             AnalyticEventsProvider analyticEventsProvider,
                             EventTransactionDb eventTransactionDb) {
        this.collectorProperties = collectorProperties;
        this.bigQueryService = bigQueryService;
        startDate = startDate(collectorProperties);
        endDate = endDate(collectorProperties);
        this.analyticEventsProvider = analyticEventsProvider;
        this.eventTransactionDb = eventTransactionDb;
        this.soccerInPlayData = loadSoccerInPlayData();
        populateIncompleteFeatures();
    }

    private void populateIncompleteFeatures() {
        for (String featuresTable : FEATURES_TABLES) {
            try {
                incompleteFeatures.put(featuresTable, incompleteFeatures(featuresTable));
            } catch (InterruptedException e) {
                log.error("Error getting incomplete features");
            }
        }
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    @SneakyThrows
    public void calcFeatures() {
        log.info("Begin EventFeaturesCalc");
        while (!eventTransactionDb.isDbAvailable()) {
            log.info("Waiting for eventTransactionDb to be ready...sleeping for 1min");
            TimeUnit.MINUTES.sleep(1);
        }
        try {
            calcFeatures(new ArrayList<String>(analyticEventsProvider.eventPeriods().keySet()));
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error occurred with processing EventFeaturesCalc", e);
        } finally {
            log.info("End EventFeaturesCalc");
        }
    }

    private void calcFeatures(List<String> events) throws InterruptedException {
        log.info("calcFeatures for {} events {}", events.size(), events);
        for (String featuresTable : FEATURES_TABLES) {
            Collection<String> eventsToUpdate = eventsToUpdate(featuresTable, events);
            eventsToUpdate.addAll(incompleteFeatures.get(featuresTable).keySet());
            eventsToUpdate.parallelStream().forEach(eventId -> updateEventFeature(eventId, soccerInPlayData, incompleteFeatures.get(featuresTable), featuresTable));
        }
    }

    private Map<String, Instant> incompleteFeatures(String featureTable) throws InterruptedException {
        final Map<String, Instant> result = new ConcurrentHashMap<>();
        Map<String, EventPeriod> eventPeriods = analyticEventsProvider.eventPeriods();
        String sql = String.format("SELECT eventId, unix_millis(max(timestamp)) as lastTs FROM `%s` where eventId in (%s) and timestamp between '%s 04:00:00 UTC' and '%s 04:00:00 UTC' group by eventId", featureTable, eventSql(collectorProperties), startDate, endDate);
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> eventFeaturesTimes = bigQueryService.performQuery(sql);
        eventFeaturesTimes.forEach(row -> {
            if (row.get(eventIdCol).isPresent()) {
                String eventId = row.get(eventIdCol).get().toString();
                EventPeriod eventTime = eventPeriods.get(eventId);
                if (eventTime != null) {
                    if (row.get("lastTs").isPresent()) {
                        Instant lastTs = Instant.ofEpochMilli(Long.parseLong(row.get("lastTs").get().toString()));
                        Instant expectedLastTs;
                        if (featureTable.contains(INPLAY_FEATURES_TABLE)) {
                            expectedLastTs = eventTime.getSecondHalfEndTime();
                        } else {
                            expectedLastTs = eventTime.getKickOffTime();
                        }
                        if (lastTs.isBefore(expectedLastTs)) {
                            result.put(eventId, lastTs);
                        }
                    }
                }
            }
        });
        return result;
    }

    private void updateEventFeature(String eventId,
                                    List<Map<String, Optional<Object>>> soccerInPlayData,
                                    Map<String, Instant> lastInsertedTimestamps,
                                    String featuresTable) {
        Map<String, EventPeriod> eventPeriods = analyticEventsProvider.eventPeriods();
        if (!isEmpty(eventPeriods) && eventId != null) {
            List<Map<String, Optional<Object>>> soccerInPlayRows = new ArrayList<>();
            if (featuresTable.contains(INPLAY_FEATURES_TABLE)) {
                soccerInPlayRows.addAll(soccerInPlayData);
            }
            Map<String, List<BetfairHistoricalRecord>> historicalDataRows = loadHistoricalData(List.of(eventId));
            log.debug("Begin updateEventFeature for feature={} eventId={}", featuresTable, eventId);
            EventPeriod eventPeriod = eventPeriods.get(eventId);
            if (eventPeriod != null) {
                Instant lastTs = lastInsertedTimestamps.get(eventId);
                Instant featureBeginTs = featuresTable.contains(INPLAY_FEATURES_TABLE) ? eventPeriod.getKickOffTime() : eventPeriod.getKickOffTime().minus(4, HOURS);
                Instant currentTimeStamp = lastTs != null ? lastTs.plus(30, SECONDS) : featureBeginTs;
                Instant cutOff = featuresTable.contains(INPLAY_FEATURES_TABLE) ? eventPeriod.getSecondHalfEndTime() : eventPeriod.getKickOffTime();
                if (currentTimeStamp.isAfter(cutOff)) {
                    currentTimeStamp = cutOff;
                }
                int tsCounter = 0;
                while (currentTimeStamp.isBefore(cutOff) || currentTimeStamp.equals(cutOff)) {
                    if (featuresTable.contains(INPLAY_FEATURES_TABLE)) {
                        insertInPlayFeatures(soccerInPlayRows, historicalDataRows.get(eventId), eventId, eventPeriod, currentTimeStamp);
                    } else {
                        insertPreLiveFeatures(historicalDataRows.get(eventId), eventId, eventPeriod, currentTimeStamp);
                    }
                    if (currentTimeStamp.equals(cutOff)) {
                        break;
                    }
                    currentTimeStamp = currentTimeStamp.plus(30, SECONDS);
                    if (currentTimeStamp.isAfter(cutOff)) {
                        currentTimeStamp = cutOff;
                    }
                    tsCounter++;
                }
                log.debug("End updateEventFeature for feature={} eventId={} noOfTimestamp={}", featuresTable, eventId, tsCounter);
            }
        }
    }

    private void insertPreLiveFeatures(List<BetfairHistoricalRecord> historicalDataRows, String eventId, EventPeriod eventPeriod, Instant currentTimeStamp) {
        long minToEnd = Duration.between(currentTimeStamp, eventPeriod.getSecondHalfEndTime()).toMinutes();
        List<BetfairHistoricalRecord> liquidityResult = liquidityQuery(currentTimeStamp, historicalDataRows);
        EventPreLiveFeature feature = EventPreLiveFeature.builder()
                .eventId(eventId)
                .minsToEnd(minToEnd)
                .timestamp(currentTimeStamp)
                .volumeMO(volume(liquidityResult, "1"))
                .volumeCS(volume(liquidityResult, "300"))
                .volumeOU05(volume(liquidityResult, "2051"))
                .volumeOU15(volume(liquidityResult, "2151"))
                .volumeOU25(volume(liquidityResult, "2251"))
                .volumeOU35(volume(liquidityResult, "2231"))
                .volumeAH(volume(liquidityResult, "41100"))
                .build();
        log.debug("Adding preLiveFeature bg={}", feature.toString());
        bigQueryService.insertRows(RESEARCH_DATASET, PRELIVE_FEATURES_TABLE, Collections.singletonList(feature.toString()));
    }

    private void insertInPlayFeatures(List<Map<String, Optional<Object>>> soccerInPlayRows,
                                      List<BetfairHistoricalRecord> historicalDataRows,
                                      String eventId,
                                      EventPeriod eventPeriod,
                                      Instant currentTimeStamp) {
        long minToEnd = Duration.between(currentTimeStamp, eventPeriod.getSecondHalfEndTime()).toMinutes();
        long secInPlay = Duration.between(eventPeriod.getKickOffTime(), currentTimeStamp).toSeconds();
        List<Map<String, Optional<Object>>> soccerInPlayByTimeStamp = filterByEventIdTimeStamp(soccerInPlayRows, eventId, currentTimeStamp);
        List<Map<String, Optional<Object>>> home = soccerInPlayByTimeStamp.stream()
                .filter(row -> row.get(teamCol).isPresent() && HOME.equals(row.get(teamCol).get().toString()))
                .collect(Collectors.toList());
        List<Map<String, Optional<Object>>> away = soccerInPlayByTimeStamp.stream()
                .filter(row -> row.get(teamCol).isPresent() && AWAY.equals(row.get(teamCol).get().toString()))
                .collect(Collectors.toList());
        List<String> scores = scores(soccerInPlayByTimeStamp);
        List<BetfairHistoricalRecord> liquidityResult = liquidityQuery(currentTimeStamp, historicalDataRows);
        EventInplayFeature feature = EventInplayFeature.builder()
                .eventId(eventId)
                .secondsInPlay(secInPlay)
                .minsToEnd(minToEnd)
                .timestamp(currentTimeStamp)
                .cumYCardsH(cumulativeUpdateTypeForTeam(home, YELLOW_CARD))
                .cumYCardsA(cumulativeUpdateTypeForTeam(away, YELLOW_CARD))
                .cumRCardsH(cumulativeUpdateTypeForTeam(home, RED_CARD))
                .cumRCardsA(cumulativeUpdateTypeForTeam(away, RED_CARD))
                .cumGoalsH(cumulativeUpdateTypeForTeam(home, GOAL))
                .cumGoalsA(cumulativeUpdateTypeForTeam(away, GOAL))
                .score(scores.get(scores.size() - 1))
                .previousScore(previousScore(scores))
                .volumeMO(volume(liquidityResult, "1"))
                .volumeCS(volume(liquidityResult, "300"))
                .volumeOU05(volume(liquidityResult, "2051"))
                .volumeOU15(volume(liquidityResult, "2151"))
                .volumeOU25(volume(liquidityResult, "2251"))
                .volumeOU35(volume(liquidityResult, "2231"))
                .volumeAH(volume(liquidityResult, "41100"))
                .build();
        log.debug("Adding inPlayFeature bg={}", feature.toString());
        bigQueryService.insertRows(RESEARCH_DATASET, INPLAY_FEATURES_TABLE, Collections.singletonList(feature.toString()));
    }

    private Map<String, List<BetfairHistoricalRecord>> loadHistoricalData(List<String> events) {
        Map<String, List<BetfairHistoricalRecord>> result = Collections.emptyMap();
        try {
            result = events.stream()
                    .map(event -> eventTransactionDb.getTransactions(event))
                    .flatMap(Collection::stream)
                    .filter(betfairHistoricalRecord -> betfairHistoricalRecord.getAsbSelectionId() != null)
                    .filter(betfairHistoricalRecord -> ASB_SELECTION_IDS.contains(betfairHistoricalRecord.getAsbSelectionId()))
                    .collect(groupingBy(BetfairHistoricalRecord::getEventId));
        } finally {
            log.info("loadHistoricalData totalRows={} for {}", result.values().stream().mapToLong(List::size).sum(), result.keySet());
        }
        return result;
    }

    private List<Map<String, Optional<Object>>> loadSoccerInPlayData() {
        log.info("Begin loadSoccerInPlayData");
        String sql = String.format("select * from " +
                "(" +
                "  SELECT eventId, unix_millis(updateTime) as updateTime, updateType, team, score  FROM `betstore.betfair_soccer_inplay` where  eventId in (select distinct eventId from `research.inplay_event_details` where source = 'Betfair' or source is null and eventId in (%s)) " +
                "  union distinct " +
                "  ( " +
                "      SELECT M.betfairEventId as eventId, unix_millis(S.updateTime) as updateTime, S.updateType, S.team, S.score  " +
                "      FROM `betstore.sofascore_soccer_inplay` S " +
                "      JOIN `betstore.betfair_sofascore_event_mapping` M " +
                "      ON S.eventId = M.sofascoreEventId " +
                "      where M.betfairEventId in ( select distinct eventId from `research.inplay_event_details` where source = 'SofaScore') and M.betfairEventId in (%s) " +
                "  ) " +
                ") order by eventId, updateTime", eventSql(collectorProperties), eventSql(collectorProperties));
        List<Map<String, Optional<Object>>> result = Collections.emptyList();
        try {
            log.info("sql={}", sql);
            result = bigQueryService.performQuery(sql)
                    .stream()
                    .peek(row -> {
                        if (row.get(updateTimeCol).isPresent()) {
                            Instant updateTimeInst = Instant.ofEpochMilli(Long.parseLong(row.get(updateTimeCol).get().toString()));
                            row.put(updateTimeCol, Optional.of(updateTimeInst));
                        }
                    })
                    .collect(Collectors.toList());
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(String.format("Error occurred executing sql=%s", sql), e);
        } finally {
            log.info("End loadSoccerInPlayData with {} rows", result.size());
        }
    }

    private Double volume(List<BetfairHistoricalRecord> liquidityResult, String asbSelectionId) {
        Double result = null;
        if (!isEmpty(liquidityResult)) {
            Optional<BetfairHistoricalRecord> searchResult = liquidityResult.stream()
                    .filter(row -> row.getAsbSelectionId() != null && row.getAsbSelectionId().equals(asbSelectionId))
                    .findFirst();
            if (searchResult.isPresent() && searchResult.get().getTotalMatched() != null) {
                result = searchResult.get().getTotalMatched();
            }
        }
        return result;
    }

    private List<BetfairHistoricalRecord> liquidityQuery(Instant timeStamp, List<BetfairHistoricalRecord> historicalData) {
        if (!isEmpty(historicalData)) {
            return historicalData.stream()
                    .filter(row -> filterByPublishTime(timeStamp, row))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String previousScore(List<String> scores) {
        if (scores.size() == 1) {
            return scores.get(0);
        }
        int previousScoreIndex = 0;
        int currentScoreIndex = scores.size() - 1;
        for (int i = currentScoreIndex - 1; i > 0; i--) {
            if (!scores.get(i).equals(scores.get(currentScoreIndex))) {
                previousScoreIndex = i;
                break;
            }
        }
        return scores.get(previousScoreIndex);
    }

    private List<String> scores(List<Map<String, Optional<Object>>> filterByTimeStamp) {
        return filterByTimeStamp.stream()
                .filter(row -> row.get(scoreCol).isPresent())
                .map(row -> row.get(scoreCol).get().toString()).collect(Collectors.toList());
    }

    private List<Map<String, Optional<Object>>> filterByEventIdTimeStamp(List<Map<String, Optional<Object>>> soccerInplayRows, String eventId, Instant timeStamp) {
        return soccerInplayRows.stream()
                .filter(row -> filterByEventIdPredicate(eventId, row))
                .filter(row -> filterByUpdateTimePredicate(timeStamp, row))
                .collect(Collectors.toList());
    }

    private Integer cumulativeUpdateTypeForTeam(List<Map<String, Optional<Object>>> soccerInPlayRows, String updateType) {
        List<Map<String, Optional<Object>>> filtered = soccerInPlayRows.stream()
                .filter(row -> row.get(updateTypeCol).isPresent())
                .filter(row -> updateType.equals(row.get(updateTypeCol).get().toString()))
                .collect(Collectors.toList());
        return filtered.size();
    }

    private boolean filterByEventIdPredicate(String eventId, Map<String, Optional<Object>> row) {
        return row.get(eventIdCol).isPresent() && eventId.equals(row.get(eventIdCol).get().toString());
    }

    private boolean filterByPublishTime(Instant timeStamp, BetfairHistoricalRecord row) {
        if (row.getPublishTime() != null) {
            long publishTime = row.getPublishTime().toEpochMilli();
            return publishTime <= timeStamp.toEpochMilli() && publishTime > timeStamp.minus(30, MINUTES).toEpochMilli();
        }
        return false;
    }

    private boolean filterByUpdateTimePredicate(Instant timeStamp, Map<String, Optional<Object>> row) {
        if (row.get(updateTimeCol).isPresent()) {
            Instant updateTime = (Instant) row.get(updateTimeCol).get();
            return updateTime.isBefore(timeStamp) || updateTime.equals(timeStamp);
        }
        return false;
    }

    private Collection<String> eventsToUpdate(String featureTable, Collection<String> currentEvents) throws InterruptedException {
        log.info("Begin eventsToUpdate for feature={}", featureTable);
        Collection<String> result = Collections.emptySet();
        String currentEventValues = currentEvents.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
        try {
            String sql = String.format("select distinct eventId FROM `%s` where eventId in (%s) and timestamp between '%s 04:00:00 UTC' and '%s 04:00:00 UTC'", featureTable, currentEventValues, startDate, endDate);
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
            Collection<String> inFeatures = rows.stream().map(row -> row.get("eventId").orElse("").toString())
                    .filter(eventId -> !eventId.isBlank())
                    .collect(Collectors.toSet());
            result = subtract(currentEvents, inFeatures);
        } finally {
            log.info("End eventsToUpdate for feature={} returned {} events {}", featureTable, result.size(), result);
        }
        return result;
    }
}
