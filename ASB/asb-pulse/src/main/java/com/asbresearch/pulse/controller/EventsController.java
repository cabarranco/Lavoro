package com.asbresearch.pulse.controller;

import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.pulse.model.EventRequest;
import com.asbresearch.pulse.service.BetfairEventService;
import com.asbresearch.pulse.util.Constants;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiParam;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.asbresearch.pulse.util.Constants.DATE_FORMATTER;
import static com.asbresearch.pulse.util.Constants.DEFAULT_TRADING_TIME_RANGE;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toSet;

@Slf4j
@RestController
@RequestMapping(value = "/event", produces = "application/json")
class EventsController {
    @Autowired
    private BetfairEventService betfairEventService;

    @PostMapping("/marketCatalogue")
    public List<MarketCatalogue> getMarketCataloguesForSoccer(@RequestBody EventRequest eventRequest) {
        Preconditions.checkNotNull(eventRequest, "Event request cannot be null");
        List<Event> events = betfairEventService.getAllEvents(eventRequest.getTimeRange(), Constants.SOCCER, eventRequest.getIncludeCompetitions(), eventRequest.getExcludeCompetitions());
        if (!CollectionUtils.isEmpty(events)) {
            Set<String> eventIds = events.stream().map(event -> event.getId()).collect(toSet());
            return betfairEventService.getMarketCatalogues(eventIds, eventRequest.getAsbMarketCodes());
        }
        return Collections.emptyList();
    }

    @PostMapping("/competition")
    public Set<String> getCompetitions(@RequestBody TimeRange timeRange) {
        return betfairEventService.getCompetitions(timeRange == null ? DEFAULT_TRADING_TIME_RANGE : timeRange);
    }

    @GetMapping(path = "/{tradingDay}", produces = "application/json")
    public List<Event> tradingDayEvents(@ApiParam(value = "tradingDay", example = "2020-05-01") @PathVariable("tradingDay") String tradingDay) {
        Preconditions.checkNotNull(tradingDay, "TradingDay is required");
        TimeRange timeRange = timeRangeForTradingDay(tradingDay);
        List<Event> allEvents = betfairEventService.getAllEvents(timeRange, Constants.SOCCER, Collections.emptySet(), Collections.emptySet())
                .stream()
                .sorted(Comparator.comparing(Event::getOpenDate))
                .collect(Collectors.toList());
        log.info("tradingDayEvents timeRange={} size={} totalEvents={}", timeRange, allEvents.size(), allEvents);
        return allEvents;
    }

    protected TimeRange timeRangeForTradingDay(String tradingDay) {
        LocalDate localDate = LocalDate.parse(tradingDay, DATE_FORMATTER);
        ZonedDateTime from = localDate.atStartOfDay(UTC).plusHours(3);
        return new TimeRange(from.toInstant(), from.plusHours(24).toInstant());
    }
}
