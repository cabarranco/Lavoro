package com.asbresearch.betfair.inplay;

import com.asbresearch.betfair.inplay.task.BetfairLiveEventTask;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BetfairLiveEventService {
    private static final int DEFAULT_POLLING_IN_MS = 1000;

    private final BetfairReferenceClient betfairReferenceClient;
    private final Timer liveEventWorker;
    private final Map<String, Event> eventCache = new ConcurrentHashMap<>();

    public BetfairLiveEventService(BetfairReferenceClient betfairReferenceClient, BigQueryService bigQueryService) {
        this(betfairReferenceClient, DEFAULT_POLLING_IN_MS, bigQueryService);
    }

    public BetfairLiveEventService(BetfairReferenceClient betfairReferenceClient, int pollingFrequencyInMillis, BigQueryService bigQueryService) {
        this.betfairReferenceClient = betfairReferenceClient;

        liveEventWorker = new Timer("Betfair-Live-Event");
        liveEventWorker.scheduleAtFixedRate(new BetfairLiveEventTask(betfairReferenceClient, eventCache, bigQueryService), 0, pollingFrequencyInMillis);
    }

    public Set<String> getLiveEventIds() {
        return eventCache.keySet();
    }

    public Optional<Event> getEvent(String eventId) {
        Event event = eventCache.get(eventId);
        if (event == null) {
            return Optional.empty();
        }
        return Optional.of(event);
    }

    @PreDestroy
    public void stop() {
        liveEventWorker.cancel();
    }
}
