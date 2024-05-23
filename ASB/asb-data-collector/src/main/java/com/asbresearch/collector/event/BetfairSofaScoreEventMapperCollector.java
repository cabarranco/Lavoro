package com.asbresearch.collector.event;

import com.asbresearch.betfair.inplay.BetfairLiveEventService;
import com.asbresearch.betfair.inplay.model.AsbResearchEvent;
import com.asbresearch.betfair.inplay.model.AsbResearchEvent.AsbResearchEventBuilder;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.matcher.TeamNameMatcher;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.asbresearch.sofascore.inplay.SofaScoreLiveEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;

@Service
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class, EmailProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "betfairSofaScoreEventMapperCollector", havingValue = "on")
public class BetfairSofaScoreEventMapperCollector {
    private static final String MAPPING_EXCEPTIONS_SQL = "SELECT id, UNIX_SECONDS(startTime) as startTime, countryCode, homeTeam, awayTeam, name FROM `betstore.betfair_events` where date( startTime ) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY) " +
            "and id not in (SELECT distinct betfairEventId  FROM `betstore.betfair_sofascore_event_mapping` where date(createTimestamp) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)) " +
            "and id in ( SELECT distinct eventId FROM `betstore.betfair_historical_data` WHERE DATE(publishTime) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY) and inplay is true ) " +
            "and id not in (SELECT distinct eventId FROM `betstore.betfair_soccer_inplay` where date(updateTime) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY))";

    private SofaScoreLiveEventService sofaScoreLiveEventService;
    private BetfairLiveEventService betfairLiveEventService;
    private final BigQueryService bigQueryService;
    private final BetfairReferenceClient betfairReferenceClient;
    private Timer liveEventWorker;
    private final CollectorProperties collectorProperties;
    private final TeamNameMatcher teamNameMatcher;
    private final EmailNotifier emailNotifier;
    private final EmailProperties emailProperties;

    @Autowired
    public BetfairSofaScoreEventMapperCollector(BigQueryService bigQueryService,
                                                BetfairReferenceClient betfairReferenceClient,
                                                CollectorProperties collectorProperties,
                                                TeamNameMatcher teamNameMatcher,
                                                EmailNotifier emailNotifier,
                                                EmailProperties emailProperties) {
        this.bigQueryService = bigQueryService;
        this.betfairReferenceClient = betfairReferenceClient;
        this.collectorProperties = collectorProperties;
        this.teamNameMatcher = teamNameMatcher;
        this.emailNotifier = emailNotifier;
        this.emailProperties = emailProperties;
    }

    @PostConstruct
    public void startEventMapping() {
        notifyOnMissingMappings();
        betfairLiveEventService = new BetfairLiveEventService(betfairReferenceClient, collectorProperties.getLiveEventFrequencyInSec() * 1000, bigQueryService);
        sofaScoreLiveEventService = new SofaScoreLiveEventService(bigQueryService, collectorProperties.getLiveEventFrequencyInSec() * 1000);

        liveEventWorker = new Timer("Event-Mapper");
        BetfairSofaScoreEventMapperTask task = new BetfairSofaScoreEventMapperTask(sofaScoreLiveEventService,
                betfairLiveEventService,
                bigQueryService,
                teamNameMatcher,
                collectorProperties.getStartTimeThresholdInSec());
        liveEventWorker.scheduleAtFixedRate(task, 0, collectorProperties.getLiveEventFrequencyInSec() * 1000);
    }

    private void notifyOnMissingMappings() {
        try {
            List<String> missingMappings = bigQueryService.performQuery(MAPPING_EXCEPTIONS_SQL)
                    .stream()
                    .map(row -> {
                        AsbResearchEventBuilder builder = AsbResearchEvent.builder().id(row.get("id").get().toString());
                        row.get("countryCode").ifPresent(o -> builder.countryCode(o.toString()));
                        row.get("startTime").ifPresent(o -> builder.startTime(Instant.ofEpochSecond(Long.valueOf(row.get("startTime").get().toString()))));
                        row.get("homeTeam").ifPresent(o -> builder.homeTeam(o.toString()));
                        row.get("awayTeam").ifPresent(o -> builder.awayTeam(o.toString()));
                        row.get("name").ifPresent(o -> builder.name(o.toString()));
                        builder.createTimestamp(Instant.now());
                        return builder.build().toString();
                    })
                    .collect(Collectors.toList());
            if (!missingMappings.isEmpty()) {
                LocalDate yesterday = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()).minus(1, ChronoUnit.DAYS);
                bigQueryService.insertRows(BETSTORE_DATASET, "betfair_events_mapping_exceptions", missingMappings);
                log.info("{} missing mappings for date={}", missingMappings.size(), yesterday);
                if (emailProperties.isNotification()) {
                    String content = missingMappings.stream().collect(Collectors.joining("\n"));
                    emailNotifier.sendMessageAsync(content, String.format("%d Betfair-SofaScore missing mappings for %s", missingMappings.size(), yesterday), emailProperties.getTo());
                }
            }
        } catch (InterruptedException e) {
            log.error("Error getting mapping data from BQ", e);
        }
    }

    @PreDestroy
    public void stopEventMapping() {
        if (betfairLiveEventService != null) {
            betfairLiveEventService.stop();
        }
        if (sofaScoreLiveEventService != null) {
            sofaScoreLiveEventService.stop();
        }
    }
}
