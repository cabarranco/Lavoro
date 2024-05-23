package com.asbresearch.sofascore.inplay.task;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.SofaScoreLiveEventClient;
import com.asbresearch.sofascore.inplay.model.AsbResearchEvent;
import com.asbresearch.sofascore.inplay.model.SofaScoreEvent;
import com.asbresearch.sofascore.inplay.model.SofaScoreLiveEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class SofaScoreLiveEventTask extends TimerTask {
    private final SofaScoreLiveEventClient sofaScoreLiveEventClient;
    private final Map<Long, SofaScoreEvent> liveEventCache;
    private final BigQueryService bigQueryService;
    private final Map<Long, Boolean> loadedEventCache = new ConcurrentHashMap<>();

    public SofaScoreLiveEventTask(BigQueryService bigQueryService, SofaScoreLiveEventClient sofaScoreLiveEventClient, Map<Long, SofaScoreEvent> liveEventCache) {
        this.sofaScoreLiveEventClient = sofaScoreLiveEventClient;
        this.liveEventCache = liveEventCache;
        this.bigQueryService = bigQueryService;
        loadEventCache();
    }

    private void loadEventCache() {
        try {
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery("SELECT id FROM `betstore.sofascore_events` where date( startTime ) = current_date() ");
            rows.forEach(row -> row.get("id").ifPresent(o -> loadedEventCache.putIfAbsent(Long.valueOf(o.toString()), Boolean.TRUE)));
            log.info("Loaded {} events from DB into cache", rows.size());
        } catch (InterruptedException e) {
            log.warn("Errors loading existing SofaScore events", e);
        }
    }

    @Override
    public void run() {
        try {
            SofaScoreLiveEvent liveEvent = sofaScoreLiveEventClient.getLiveEvent();
            log.info("Currently {} events", liveEvent.getEvents().size());
            List<SofaScoreEvent> toProcess = liveEvent.getEvents().stream().filter(event -> !liveEventCache.containsKey(event.getId())).collect(Collectors.toList());
            if (bigQueryService != null) {
                List<String> rows = toProcess.stream()
                        .filter(event -> !loadedEventCache.containsKey(event.getId()))
                        .map(event -> AsbResearchEvent.builder()
                                .id(String.valueOf(event.getId()))
                                .countryCode(event.getTournament().getCategory().getAlpha2())
                                .startTime(Instant.ofEpochSecond(event.getStartTimestamp()))
                                .homeTeam(event.getHomeTeam().getName())
                                .awayTeam(event.getAwayTeam().getName())
                                .createTimestamp(Instant.now())
                                .build()
                                .toString())
                        .collect(Collectors.toList());
                bigQueryService.insertRows("betstore", "sofascore_events", rows);
            }
            toProcess.forEach(event -> {
                liveEventCache.putIfAbsent(event.getId(), event);
                log.info("{} startTime={}", event, Instant.ofEpochSecond(event.getStartTimestamp()));
            });
        } catch (RuntimeException ex) {
            log.error("Error reading live events from SofaScore", ex);
        }
    }
}
