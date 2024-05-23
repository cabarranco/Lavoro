package com.asbresearch.collector.betfair;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.*;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.util.Helpers;
import com.asbresearch.collector.betfair.mapping.AsbSelectionMapping;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service("EventsOfTheDayProvider")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "eventsOfTheDayProvider", havingValue = "on")
@DependsOn({"BetfairReferenceClient"})
public class EventsOfTheDayProvider {
    private final BetfairReferenceClient betfairReferenceClient;
    private final Map<String, Event> eventsOfTheDay = new ConcurrentHashMap<>();
    private final CollectorProperties collectorProperties;
    private final Map<String, MarketCatalogue> marketCatalogue = new ConcurrentHashMap<>();

    @Autowired
    public EventsOfTheDayProvider(BetfairReferenceClient betfairReferenceClient,
                                  CollectorProperties collectorProperties) {
        this.betfairReferenceClient = betfairReferenceClient;
        this.collectorProperties = collectorProperties;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        loadEventsOfTheDay().forEach(event -> eventsOfTheDay.putIfAbsent(event.getId(), event));
        loadMarketCatalogueOfTheDay().forEach(e -> marketCatalogue.putIfAbsent(e.getMarketId(), e));
    }

    private List<MarketCatalogue> loadMarketCatalogueOfTheDay() {
        Collection<List<String>> batchedEvents = com.asbresearch.collector.util.CollectionUtils.partitionBasedOnSize(eventsOfTheDay.values().stream().map(Event::getId).collect(Collectors.toSet()), 10);
        return batchedEvents.stream()
                .map(events -> {
                    List<MarketCatalogue> result = new ArrayList<>();
                    MarketFilter marketFilter = new MarketFilter();
                    marketFilter.setMarketTypeCodes(Constants.getMarketTypeCodes(collectorProperties));
                    marketFilter.setEventIds(new HashSet<>(events));
                    BetfairServerResponse<List<MarketCatalogue>> serverResponse = betfairReferenceClient.listMarketCatalogue(marketFilter, Helpers.soccerMatchProjection(), MarketSort.FIRST_TO_START, 400);
                    if (serverResponse != null && serverResponse.getResponse() != null) {
                        result.addAll(serverResponse.getResponse()
                                .stream()
                                .filter(marketCatalogue -> AsbSelectionMapping.marketType.keySet().contains(marketCatalogue.getMarketName()))
                                .filter(this::isValidMarketCatalogue)
                                .collect(Collectors.toList()));
                    }
                    return result;
                })
                .flatMap(catalogues -> catalogues.stream())
                .collect(Collectors.toList());
    }

    private boolean isValidMarketCatalogue(MarketCatalogue marketCatalogue) {
        if (CollectionUtils.isEmpty(marketCatalogue.getRunners())) {
            return false;
        }
        List<RunnerCatalog> runners = marketCatalogue.getRunners();
        for (RunnerCatalog runner : runners) {
            if (runner == null) {
                return false;
            }
        }
        if (marketCatalogue.getCompetition() == null) {
            return false;
        }
        if (marketCatalogue.getEvent() == null) {
            return false;
        }
        return true;
    }

    private Set<Event> loadEventsOfTheDay() {
        MarketFilter marketFilter = new MarketFilter();
        marketFilter.setEventTypeIds(Set.of("1"));
        marketFilter.setMarketStartTime(Constants.TimeRangeForTradingDay);
        BetfairServerResponse<List<EventResult>> serverResponse = betfairReferenceClient.listEvents(marketFilter);
        if (serverResponse != null && !CollectionUtils.isEmpty(serverResponse.getResponse())) {
            return serverResponse.getResponse().stream().map(eventResult -> eventResult.getEvent()).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public Collection<Event> getEventsOfTheDay() {
        return eventsOfTheDay.values();
    }

    public Collection<MarketCatalogue> getMarketCatalogueOfTheDay() {
        return marketCatalogue.values();
    }
}
