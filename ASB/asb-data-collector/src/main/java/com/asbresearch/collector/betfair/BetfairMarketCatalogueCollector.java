package com.asbresearch.collector.betfair;

import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.RunnerCatalog;
import com.asbresearch.collector.betfair.mapping.AsbSelectionMapping;
import com.asbresearch.collector.betfair.model.BetfairMarketCatalogueRecord;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.asbresearch.collector.util.Constants.*;
import static com.asbresearch.common.BigQueryUtil.BETFAIR_MARKET_CATALOGUE_TABLE;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;

@Service
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "marketCatalogueCollector", havingValue = "on")
public class BetfairMarketCatalogueCollector {
    private final BigQueryService bigQueryService;
    private final EventsOfTheDayProvider eventsOfTheDayProvider;
    private final Map<String, Boolean> existingCatalogueCache = new ConcurrentHashMap<>();

    @Autowired
    public BetfairMarketCatalogueCollector(BigQueryService bigQueryService,
                                           EventsOfTheDayProvider eventsOfTheDayProvider) {
        this.bigQueryService = bigQueryService;
        this.eventsOfTheDayProvider = eventsOfTheDayProvider;
        existingCatalogueCache.putAll(getExistingCatalogue());
    }

    @Scheduled(fixedDelay = 120000)
    public void publishMarketCatalogue() {
        List<BetfairMarketCatalogueRecord> marketCatalogueRecord = toMarketCatalogueRecord(eventsOfTheDayProvider.getMarketCatalogueOfTheDay());
        if (!marketCatalogueRecord.isEmpty()) {
            List<String> rows = marketCatalogueRecord.stream().map(record -> record.toString()).collect(Collectors.toList());
            bigQueryService.insertRows(BETSTORE_DATASET, BETFAIR_MARKET_CATALOGUE_TABLE, rows);
            marketCatalogueRecord.forEach(record -> existingCatalogueCache.putIfAbsent(String.format("%s_%s_%s", record.getEventId(), record.getMarketId(), record.getAsbSelectionId()), Boolean.TRUE));
        }
    }

    private Map<String, Boolean> getExistingCatalogue() {
        Map<String, Boolean> existingCatalogueCache = new HashMap<>();
        String sql = String.format("select eventId,marketId,asbSelectionId FROM `%s.betfair_market_catalogue` where unix_millis(startTime) between %s and %s and DATE(startTime) >= CURRENT_DATE()",
                BETSTORE_DATASET,
                TimeRangeForTradingDay.getFrom().toEpochMilli(),
                TimeRangeForTradingDay.getTo().toEpochMilli());
        try {
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
            resultSet.forEach(row -> {
                Optional<Object> eventId = row.get("eventId");
                Optional<Object> marketId = row.get("marketId");
                Optional<Object> asbSelectionId = row.get("asbSelectionId");
                if (eventId.isPresent() && marketId.isPresent() && asbSelectionId.isPresent()) {
                    existingCatalogueCache.putIfAbsent(String.format("%s_%s_%s", eventId.get(), marketId.get(), asbSelectionId.get()), Boolean.TRUE);
                }
            });
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for data from BigQuery betfair..", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for data from BigQuery betfair..");
        }
        return existingCatalogueCache;
    }

    private List<BetfairMarketCatalogueRecord> toMarketCatalogueRecord(Collection<MarketCatalogue> marketCatalogues) {
        List<BetfairMarketCatalogueRecord> result = new ArrayList<>();
        for (MarketCatalogue marketCatalogue : marketCatalogues) {
            List<RunnerCatalog> runners = marketCatalogue.getRunners();
            int count = 0;
            for (RunnerCatalog runner : runners) {
                String runnerName = runner.getRunnerName();
                if (isMatchOdds(marketCatalogue)) {
                    switch (count) {
                        case 0:
                            runnerName = "Home";
                            break;
                        case 1:
                            runnerName = "Away";
                            break;
                        case 2:
                            runnerName = "Draw";
                            break;
                    }
                }
                count++;
                if (isAsianHandicap(marketCatalogue)) {
                    double handicap = runner.getHandicap();
                    if (handicap == 1.5 || handicap == -1.5 || handicap == 0.5 || handicap == -0.5) {
                        if (marketCatalogue.getEvent().getName().startsWith(runnerName)) {
                            runnerName = "Home";
                        }
                        if (marketCatalogue.getEvent().getName().endsWith(runnerName)) {
                            runnerName = "Away";
                        }
                        if ("Home".equals(runnerName) || "Away".equalsIgnoreCase(runnerName)) {
                            runnerName = runnerName + " AH " + ((handicap) > 0 ? "+" + handicap : handicap);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                String asbSelectionId = asbSelectionId(marketCatalogue.getMarketName(), runnerName);
                if (asbSelectionId != null) {
                    if (!existingCatalogueCache.containsKey(String.format("%s_%s_%s", marketCatalogue.getEvent().getId(), marketCatalogue.getMarketId(), asbSelectionId))) {
                        BetfairMarketCatalogueRecord record = BetfairMarketCatalogueRecord.builder()
                                .competition(marketCatalogue.getCompetition().getName())
                                .eventId(marketCatalogue.getEvent().getId())
                                .eventName(marketCatalogue.getEvent().getName())
                                .marketName(marketCatalogue.getMarketName())
                                .marketId(marketCatalogue.getMarketId())
                                .runnerName(runnerName)
                                .asbSelectionId(asbSelectionId)
                                .selectionId(String.valueOf(runner.getSelectionId()))
                                .startTime(marketCatalogue.getEvent().getOpenDate())
                                .build();
                        result.add(record);
                    }
                }
            }
        }
        return result;
    }

    private boolean isAsianHandicap(MarketCatalogue marketCatalogue) {
        return ASIAN_HANDICAP.equalsIgnoreCase(marketCatalogue.getMarketName()) || ASIAN_HANDICAP_UNMANAGED.equalsIgnoreCase(marketCatalogue.getMarketName());
    }

    private boolean isMatchOdds(MarketCatalogue marketCatalogue) {
        return MATCH_ODDS.equalsIgnoreCase(marketCatalogue.getMarketName()) || MATCH_ODDS_UNMANAGED.equalsIgnoreCase(marketCatalogue.getMarketName());
    }

    private String asbSelectionId(String marketName, String runnerName) {
        Map<String, Integer> marketSelections = AsbSelectionMapping.selections.get(marketName);
        if (marketSelections != null) {
            Integer selection = marketSelections.get(runnerName);
            if (selection != null) {
                return String.valueOf(selection);
            }
        }
        return null;
    }
}
