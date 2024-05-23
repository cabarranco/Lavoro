package com.asbresearch.collector.event;

import com.asbresearch.betfair.inplay.BetfairLiveEventService;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.collector.event.model.BetfairSofaScoreMappingRecord;
import com.asbresearch.collector.matcher.TeamNameMatcher;
import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.SofaScoreLiveEventService;
import com.asbresearch.sofascore.inplay.model.SofaScoreEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Slf4j
class BetfairSofaScoreEventMapperTask extends TimerTask {
    private final SofaScoreLiveEventService sofaScoreLiveEventService;
    private final BetfairLiveEventService betfairLiveEventService;
    private final BigQueryService bigQueryService;
    private final Map<String, String> existingEventCache = new ConcurrentHashMap<>();
    private final TeamNameMatcher teamNameMatcher;
    private final TeamNameIgnoreTokenRemover tokenRemover = new TeamNameIgnoreTokenRemover();
    private final long startTimeThresholdInSec;

    BetfairSofaScoreEventMapperTask(SofaScoreLiveEventService sofaScoreLiveEventService,
                                    BetfairLiveEventService betfairLiveEventService,
                                    BigQueryService bigQueryService,
                                    TeamNameMatcher teamNameMatcher,
                                    int startTimeThresholdInSec) {
        this.sofaScoreLiveEventService = sofaScoreLiveEventService;
        this.betfairLiveEventService = betfairLiveEventService;
        this.bigQueryService = bigQueryService;
        this.teamNameMatcher = teamNameMatcher;
        this.startTimeThresholdInSec = startTimeThresholdInSec;
        preLoadExistingEvents();
        log.info("Start Betfair-SofaScore mapper using startTimeThresholdInSec={}", startTimeThresholdInSec);
    }

    private void preLoadExistingEvents() {
        try {
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery("select betfairEventId, sofascoreEventId from `betstore.betfair_sofascore_event_mapping` where date(createTimestamp) = current_date()");
            rows.forEach(row -> existingEventCache.putIfAbsent(row.get("betfairEventId").get().toString(), row.get("sofascoreEventId").get().toString()));
            log.info("Loaded {} event mapping into the cache", rows.size());
        } catch (RuntimeException | InterruptedException e) {
            log.warn("Error while trying to load events mapping from bigQuery", e);
        }
    }

    @Override
    public void run() {
        try {
            List<String> betfairEvents = betfairLiveEventService.getLiveEventIds().stream().filter(s -> !existingEventCache.containsKey(s)).collect(Collectors.toList());
            betfairEvents.forEach(betfairEventId -> {
                Optional<Event> betfairOptEvent = betfairLiveEventService.getEvent(betfairEventId);
                betfairOptEvent.ifPresent(this::tryMap);
            });
        } catch (RuntimeException ex) {
            log.error("Error trying to map event-ids", ex);
        }
    }

    private void tryMap(Event betfairEvent) {
        log.debug("Mapping betfair event={}", betfairEvent.getName());
        List<SofaScoreEvent> sofaScoreEvents = sofaScoreLiveEventService.getEvent(betfairEvent.getOpenDate(), startTimeThresholdInSec);
        List<SofaScoreEvent> sameCountryEvents = sofaScoreEvents.stream().filter(event -> event.getTournament().getCategory().getAlpha2() != null &&
                event.getTournament().getCategory().getAlpha2().equals(betfairEvent.getCountryCode()) || ("EN".equals(event.getTournament().getCategory().getAlpha2()) && "GB".equals(betfairEvent.getCountryCode()))).collect(Collectors.toList());
        Collection<SofaScoreEvent> otherEvents = CollectionUtils.subtract(sofaScoreEvents, sameCountryEvents);
        if (tryMap(betfairEvent, sameCountryEvents, (left, right) -> left && right)) {
            return;
        }
        if (tryMap(betfairEvent, otherEvents, (left, right) -> left && right)) {
            return;
        }
        log.warn("Unable to map event {}, pls investigate", betfairEvent);
    }

    private boolean tryMap(Event betfairEvent, Collection<SofaScoreEvent> sofaScoreEvents, BiPredicate<Boolean, Boolean> isTeamSimilarChecker) {
        String[] tokens = betfairEvent.getName().split("\\s+v\\s+");
        if (tokens.length != 2) {
            return false;
        }
        String betfairHomeTeam = tokens[0];
        String betfairAwayTeam = tokens[1];
        for (SofaScoreEvent sofaScoreEvent : sofaScoreEvents) {
            if (!existingEventCache.containsValue(String.valueOf(sofaScoreEvent.getId()))) {
                log.debug("Matching {} vs {} startTime={}", betfairEvent, sofaScoreEvent, Instant.ofEpochSecond(sofaScoreEvent.getStartTimestamp()));
                String sofaScoreHomeTeam = sofaScoreEvent.getHomeTeam().getSlug().replace('-', ' ');
                boolean isHomeTeamSimilar = teamNameMatcher.isSameTeam(removeCommonToken(betfairHomeTeam), removeCommonToken(sofaScoreHomeTeam));
                String sofaScoreAwayTeam = sofaScoreEvent.getAwayTeam().getSlug().replace('-', ' ');
                boolean isAwayTeamSimilar = teamNameMatcher.isSameTeam(removeCommonToken(betfairAwayTeam), removeCommonToken(sofaScoreAwayTeam));
                log.debug("betfairEventId={} isHomeTeamSimilar={} isAwayTeamSimilar={} bHome={} cHome={} bAway={} cAway={}",
                        betfairEvent.getId(), isHomeTeamSimilar, isAwayTeamSimilar, betfairHomeTeam, sofaScoreEvent.getHomeTeam().getName(), betfairAwayTeam, sofaScoreEvent.getAwayTeam().getName());
                if (isTeamSimilarChecker.test(isHomeTeamSimilar, isAwayTeamSimilar)) {
                    log.debug("Successful betfairEventId={} isHomeTeamSimilar={} isAwayTeamSimilar={} bHome={} cHome={} bAway={} cAway={}",
                            betfairEvent.getId(), isHomeTeamSimilar, isAwayTeamSimilar, betfairHomeTeam, sofaScoreEvent.getHomeTeam().getName(), betfairAwayTeam, sofaScoreEvent.getAwayTeam().getName());
                    BetfairSofaScoreMappingRecord mappingRecord = BetfairSofaScoreMappingRecord.builder()
                            .betfairEventId(betfairEvent.getId())
                            .sofascoreEventId(String.valueOf(sofaScoreEvent.getId()))
                            .createTimestamp(Instant.now())
                            .build();
                    bigQueryService.insertRow(BigQueryUtil.BETSTORE_DATASET, "betfair_sofascore_event_mapping", mappingRecord.toString());
                    existingEventCache.putIfAbsent(betfairEvent.getId(), String.valueOf(sofaScoreEvent.getId()));
                    return true;
                }
            }
        }
        return false;
    }

    private String removeCommonToken(String teamName) {
        return tokenRemover.removeIgnoreToken(teamName);
    }
}
