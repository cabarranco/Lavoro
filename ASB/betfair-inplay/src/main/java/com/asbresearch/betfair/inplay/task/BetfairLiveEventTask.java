package com.asbresearch.betfair.inplay.task;

import com.asbresearch.betfair.inplay.model.AsbResearchEvent;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.EventResult;
import com.asbresearch.betfair.ref.entities.MarketFilter;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

@Slf4j
public class BetfairLiveEventTask extends TimerTask {
    private final BetfairReferenceClient betfairReferenceClient;
    private final Map<String, Event> eventCache;
    private final Map<String, Boolean> loadedEventCache = new ConcurrentHashMap<>();
    private final BigQueryService bigQueryService;

    public BetfairLiveEventTask(BetfairReferenceClient betfairReferenceClient, Map<String, Event> eventCache, BigQueryService bigQueryService) {
        this.betfairReferenceClient = betfairReferenceClient;
        this.eventCache = eventCache;
        this.bigQueryService = bigQueryService;
        loadEventCache();
    }

    private void loadEventCache() {
        try {
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery("SELECT id FROM `betstore.betfair_events` where date( startTime ) = current_date()");
            rows.forEach(row -> row.get("id").ifPresent(o -> loadedEventCache.putIfAbsent(o.toString(), Boolean.TRUE)));
            log.info("Loaded {} events from DB into cache", rows.size());
        } catch (InterruptedException e) {
            log.warn("Errors loading existing Betfair events", e);
        }
    }

    @Override
    public void run() {
        try {
            Instant now = Instant.now();
            MarketFilter filter = new MarketFilter();
            filter.setEventTypeIds(singleton("1"));
            filter.setInPlayOnly(true);
            BetfairServerResponse<List<EventResult>> response = betfairReferenceClient.listEvents(filter);
            if (response != null) {
                List<EventResult> eventResults = response.getResponse();
                if (eventResults != null) {
                    List<Event> events = eventResults.stream()
                            .map(eventResult -> eventResult.getEvent())
                            .filter(event -> event.getOpenDate() != null && (event.getOpenDate().equals(now) || event.getOpenDate().isBefore(now)))
                            .filter(event -> Duration.between(now, event.getOpenDate()).abs().getSeconds() < TimeUnit.HOURS.toSeconds(4))
                            .collect(Collectors.toList());

                    log.info("Currently {} live events", events.size());
                    List<Event> toProcess = events.stream().filter(event -> !eventCache.containsKey(event.getId())).collect(Collectors.toList());
                    if (bigQueryService != null) {
                        List<String> rows = toProcess.stream()
                                .filter(event -> !loadedEventCache.containsKey(event.getId()))
                                .map(event -> {
                                    String[] teams = event.getName().split("\\s+v\\s+");
                                    String homeTeam = null;
                                    String awayTeam = null;
                                    if (teams.length == 2) {
                                        homeTeam = teams[0];
                                        awayTeam = teams[1];
                                    }
                                    return AsbResearchEvent.builder()
                                            .id(String.valueOf(event.getId()))
                                            .countryCode(event.getCountryCode())
                                            .startTime(event.getOpenDate())
                                            .homeTeam(homeTeam)
                                            .awayTeam(awayTeam)
                                            .name(event.getName())
                                            .createTimestamp(Instant.now())
                                            .build()
                                            .toString();
                                })
                                .collect(Collectors.toList());
                        bigQueryService.insertRows("betstore", "betfair_events", rows);
                    }

                    toProcess.forEach(event -> {
                        log.info(" eventId={} name={} countryCode={} startTime={}",
                                event.getId(),
                                event.getName(),
                                event.getCountryCode(),
                                event.getOpenDate());
                        eventCache.putIfAbsent(event.getId(), event);
                    });
                }
            }
        } catch (RuntimeException ex) {
            log.error("Error reading live events from betfair", ex);
        }
    }
}
