package com.asbresearch.collector.betfair;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.inplay.model.InPlayRequest;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Service
@EnableConfigurationProperties({CollectorProperties.class})
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "soccerInPlayCollector", havingValue = "on")
public class SoccerInPlayCollector {
    private final EventsOfTheDayProvider eventsOfTheDayProvider;
    private final BetfairInPlayService betfairInPlayService;
    private final BigQueryService bigQueryService;

    @Autowired
    public SoccerInPlayCollector(BigQueryService bigQueryService,
                                 EventsOfTheDayProvider eventsOfTheDayProvider,
                                 CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        this.eventsOfTheDayProvider = eventsOfTheDayProvider;
        betfairInPlayService = new BetfairInPlayService(
                (int) TimeUnit.SECONDS.toMillis(collectorProperties.getBetfairInPlayPollingFrequencyInSec()),
                bigQueryService,
                collectorProperties.isBetfairInPlaySave(),
                false,
                collectorProperties.getFeignLoggerLevel());
    }

    @PostConstruct
    public void subscribeAtStartUp() {
        String query = "select eventId, unix_millis(startTime) as startTime from `betstore.betfair_market_catalogue` WHERE startTime > TIMESTAMP_SUB(CURRENT_TIMESTAMP(),INTERVAL 180 MINUTE) AND asbSelectionId = '1' AND eventId in " +
                "( " +
                "SELECT distinct eventId FROM `betstore.betfair_market_catalogue` WHERE startTime > TIMESTAMP_SUB(CURRENT_TIMESTAMP(),INTERVAL 180 MINUTE) " +
                "except distinct " +
                "SELECT distinct eventId FROM `betstore.betfair_soccer_inplay` where updateType = 'SecondHalfEnd' " +
                ") ";
        log.info("sql={}", query);
        try {
            Set<InPlayRequest> inPlayRequests = bigQueryService.performQuery(query).stream()
                    .filter(row -> row.get("eventId").isPresent() && row.get("startTime").isPresent())
                    .map(row -> {
                        Integer eventId = Integer.valueOf(row.get("eventId").get().toString());
                        Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get("startTime").get().toString()));
                        return InPlayRequest.of(Integer.valueOf(eventId), startTime);
                    }).collect(Collectors.toSet());
            betfairInPlayService.requestPolling(inPlayRequests);
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error getting pending events for inPlay service sql={}", query, e);
        }
    }

    @Scheduled(fixedDelay = 120000)
    public void run() {
        Set<InPlayRequest> inPlayRequests = eventsOfTheDayProvider.getEventsOfTheDay().stream()
                .map(event -> InPlayRequest.of(Integer.valueOf(event.getId()), event.getOpenDate()))
                .collect(Collectors.toSet());
        betfairInPlayService.requestPolling(inPlayRequests);
    }

    @PreDestroy
    public void stop() {
        if (betfairInPlayService != null) {
            betfairInPlayService.stop();
        }
    }
}
