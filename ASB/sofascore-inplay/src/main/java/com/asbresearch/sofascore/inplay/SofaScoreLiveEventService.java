package com.asbresearch.sofascore.inplay;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.model.SofaScoreEvent;
import com.asbresearch.sofascore.inplay.task.SofaScoreLiveEventTask;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.asbresearch.sofascore.inplay.util.SofaScoreConstant.BASE_URL;
import static feign.Logger.Level.BASIC;

@Slf4j
public class SofaScoreLiveEventService {
    private static final int DEFAULT_POLLING_IN_MS = 1000;

    private final Timer liveEventWorker;
    private final Map<Long, SofaScoreEvent> liveEventCache = new ConcurrentHashMap<>();

    public SofaScoreLiveEventService(BigQueryService bigQueryService) {
        this(bigQueryService, DEFAULT_POLLING_IN_MS, BASIC);
    }

    public SofaScoreLiveEventService(BigQueryService bigQueryService, int pollingFrequencyInMillis) {
        this(bigQueryService, pollingFrequencyInMillis, BASIC);
    }

    public SofaScoreLiveEventService(BigQueryService bigQueryService, int pollingFrequencyInMillis, Logger.Level loggerLevel) {
        SofaScoreLiveEventClient sofaScoreLiveEventClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(SofaScoreLiveEventClient.class))
                .logLevel(loggerLevel)
                .target(SofaScoreLiveEventClient.class, BASE_URL);
        log.info("Created with pollingFrequencyInMillis={}", pollingFrequencyInMillis);
        liveEventWorker = new Timer("SofaScore-Live-Event");
        liveEventWorker.scheduleAtFixedRate(new SofaScoreLiveEventTask(bigQueryService, sofaScoreLiveEventClient, liveEventCache), 0, pollingFrequencyInMillis);
    }

    public Set<Long> getLiveEventIds() {
        return liveEventCache.keySet();
    }

    public Optional<SofaScoreEvent> getEvent(Long eventId) {
        SofaScoreEvent event = liveEventCache.get(eventId);
        if (event == null) {
            return Optional.empty();
        }
        return Optional.of(event);
    }

    public List<SofaScoreEvent> getEvent(String betfairCountryCode, Instant betfairStartTime, long thresholdInSec) {
        return getEvent(betfairStartTime, thresholdInSec).stream()
                .filter(sofaScoreEvent -> betfairCountryCode.equals(sofaScoreEvent.getTournament().getCategory().getAlpha2()))
                .collect(Collectors.toList());
    }

    public List<SofaScoreEvent> getEventByCountry(String countryCode) {
        return liveEventCache.values()
                .stream()
                .filter(event -> countryCode.equals(event.getTournament().getCategory().getAlpha2()))
                .collect(Collectors.toList());
    }

    public List<SofaScoreEvent> getEvent(Instant betfairStartTime, long thresholdInSec) {
        Predicate<SofaScoreEvent> startTimePredicate = sofaScoreEvent -> {
            Instant sofaScoreStartTime = Instant.ofEpochSecond(sofaScoreEvent.getStartTimestamp());
            Duration diff = Duration.between(sofaScoreStartTime, betfairStartTime).abs();
            return diff.getSeconds() <= thresholdInSec;
        };

        return liveEventCache.values()
                .stream()
                .filter(startTimePredicate)
                .collect(Collectors.toList());
    }

    @PreDestroy
    public void stop() {
        liveEventWorker.cancel();
    }
}