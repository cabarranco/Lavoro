package com.asbresearch.pulse.service;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.CompetitionResult;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.EventResult;
import com.asbresearch.betfair.ref.entities.EventType;
import com.asbresearch.betfair.ref.entities.EventTypeResult;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.MarketFilter;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.enums.MarketType;
import com.asbresearch.betfair.ref.util.Helpers;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.asbresearch.betfair.ref.enums.MarketBettingType.ODDS;
import static com.asbresearch.pulse.util.Constants.DEFAULT_TRADING_TIME_RANGE;
import static com.asbresearch.pulse.util.Constants.SOCCER;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@Slf4j
public class BetfairEventService {
    private final BetfairReferenceClient betfairReferenceClient;
    private final Map<String, String> name2IdCompetitions;
    private final Map<String, String> name2IdEventTypes;
    private final BetfairMarketTypeMapping marketTypeMapping;

    @Autowired
    public BetfairEventService(BetfairReferenceClient betfairReferenceClient, BetfairMarketTypeMapping marketTypeMapping) {
        this.betfairReferenceClient = betfairReferenceClient;
        this.marketTypeMapping = marketTypeMapping;
        name2IdEventTypes = ImmutableMap.copyOf(loadEventTypes());
        name2IdCompetitions = ImmutableMap.copyOf(loadCompetitions(DEFAULT_TRADING_TIME_RANGE));
        log.info("name2IdEventTypes {}", name2IdEventTypes);
        log.info("name2IdCompetitions {}", name2IdCompetitions);
    }

    private Map<String, String> loadEventTypes() {
        BetfairServerResponse<List<EventTypeResult>> response = betfairReferenceClient.listEventTypes(new MarketFilter());
        if (response == null) {
            log.error("Error loading eventTypes from betfair");
            throw new RuntimeException("Error loading eventTypes from betfair");
        }
        List<EventType> allEvents = response.getResponse()
                .stream()
                .map(EventTypeResult::getEventType)
                .collect(toList());
        Map<String, String> result = new HashMap<>();
        allEvents.forEach(eventType -> result.put(eventType.getName(), eventType.getId()));
        return result;
    }


    private Map<String, String> loadCompetitions(TimeRange timeRange) {
        MarketFilter marketFilter = new MarketFilter();
        if (timeRange != null) {
            marketFilter.setMarketStartTime(timeRange);
            marketFilter.setEventTypeIds(Collections.singleton(name2IdEventTypes.get(SOCCER)));
        }
        BetfairServerResponse<List<CompetitionResult>> response = betfairReferenceClient.listCompetitions(marketFilter);
        if (response == null) {
            log.error("Error loading leagues from betfair");
            throw new RuntimeException("Error loading leagues from betfair");
        }
        Map<String, String> result = new HashMap<>();
        List<CompetitionResult> competitions = response.getResponse();
        if (!CollectionUtils.isEmpty(competitions)) {
            competitions.forEach(competitionResult -> result.put(competitionResult.getCompetition().getName(), competitionResult.getCompetition().getId()));
        }
        return result;
    }

    public String getEventTypeId(String eventName) {
        return name2IdEventTypes.get(eventName);
    }

    public List<Event> getAllEvents(TimeRange timeRange, String eventName, Set<String> includeCompetitions, Set<String> excludeCompetitions) {
        Set<String> competitionIds = resolveCompetitionIds(includeCompetitions, excludeCompetitions);
        if (competitionIds.isEmpty()) {
            log.warn("Resolve competition ids are empty...");
            return Collections.emptyList();
        }
        MarketFilter marketfilter = new MarketFilter();
        List<Event> result = Collections.emptyList();
        marketfilter.setMarketStartTime(timeRange);
        marketfilter.setEventTypeIds(Collections.singleton(getEventTypeId(eventName)));
        marketfilter.setCompetitionIds(competitionIds);
        BetfairServerResponse<List<EventResult>> response = betfairReferenceClient.listEvents(marketfilter);
        if (response != null && response.getResponse() != null) {
            result = response.getResponse().stream().map(EventResult::getEvent).collect(toList());
        }
        return result.stream()
                .filter(event -> event.getOpenDate().isAfter(timeRange.getFrom()) || event.getOpenDate().equals(timeRange.getFrom()))
                .collect(Collectors.toList());
    }

    public List<MarketCatalogue> getMarketCatalogues(Set<String> eventIds, Set<String> userMarketTypeCodes) {
        List<MarketCatalogue> result = new ArrayList<>();
        Set<String> marketTypeCodes = userMarketTypeCodes.stream().map(this::toBetFairMarketTypeCode).map(Enum::toString).collect(toSet());

        Iterable<List<String>> partitions = Iterables.partition(eventIds, 10);
        partitions.forEach(ids -> {
            MarketFilter filter = new MarketFilter();
            filter.setMarketBettingTypes(singleton(ODDS));
            filter.setMarketTypeCodes(marketTypeCodes);
            filter.setEventIds(new HashSet<>(ids));
            BetfairServerResponse<List<MarketCatalogue>> response = betfairReferenceClient.listMarketCatalogue(filter, Helpers.soccerMatchProjection(), MarketSort.FIRST_TO_START, 400);
            if (response != null && response.getResponse() != null) {
                result.addAll(response.getResponse());
            }
        });
        return result;
    }

    public Set<String> getCompetitions(TimeRange timeRange) {
        return loadCompetitions(timeRange).keySet();
    }

    protected MarketType toBetFairMarketTypeCode(String userMarketCode) {
        return marketTypeMapping.marketType(userMarketCode);
    }

    protected Set<String> resolveCompetitionIds(Set<String> includeCompetitionNames, Set<String> excludeCompetitionNames) {
        log.info("includeCompetitionNames={}", includeCompetitionNames);
        log.info("excludeCompetitionNames={}", excludeCompetitionNames);
        Set<String> competitionIds = new HashSet<>();
        if (includeCompetitionNames.isEmpty()) {
            competitionIds.addAll(name2IdCompetitions.values());
        } else {
            competitionIds.addAll(includeCompetitionNames.stream().map(name2IdCompetitions::get)
                    .filter(Objects::nonNull)
                    .collect(toSet()));
        }
        if (!excludeCompetitionNames.isEmpty()) {
            Set<String> excludeCompetitionIds = excludeCompetitionNames.stream().map(name2IdCompetitions::get)
                    .filter(Objects::nonNull)
                    .collect(toSet());
            competitionIds.removeAll(excludeCompetitionIds);
        }
        log.info("resolveCompetitionIds={}", competitionIds);
        return competitionIds;
    }
}
