package com.asbresearch.collector.mercurius;

import com.asbresearch.collector.config.MercuriusProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.asbresearch.collector.util.CollectionUtils.isEmpty;

@Component("MercuriusTradeProcessor")
@Slf4j
@ConditionalOnProperty(prefix = "mercurius", name = "mercuriusTradeProcessor", havingValue = "on")
@EnableConfigurationProperties({MercuriusProperties.class})
public class MercuriusTradeProcessor {
    private static final String ASB_MERCURIUS_TRADE_FILE_NAME = "mercurius_2.csv";

    private static final String MARKET_CATALOGUE_SQL = "SELECT distinct eventId, marketId, selectionId, asbSelectionId " +
            "FROM `betstore.betfair_market_catalogue` " +
            "WHERE DATE(startTime) <= CURRENT_DATE() ";

    private static final String HISTORICAL_DATA_SQL = "SELECT distinct marketId, selectionId, asbSelectionId " +
            "FROM `betstore.betfair_historical_data` " +
            "WHERE DATE(publishTime) <= CURRENT_DATE()";

    private static final String EVENT_INPLAY_FEATURES_SQL = "SELECT distinct eventId " +
            "FROM `research.event_inplay_features` " +
            "WHERE DATE(timestamp) <= CURRENT_DATE() and eventId in (%s)";

    private static final String PRICES_ANALYTICS_SQL = "SELECT eventId, asbSelectionId " +
            "FROM `research.event_prices_analytics` " +
            "WHERE DATE(timestamp) <= CURRENT_DATE() and eventId in (%s)";

    private final BigQueryService bigQueryService;
    private final File tradePath;

    @Autowired
    public MercuriusTradeProcessor(BigQueryService bigQueryService, MercuriusProperties mercuriusProperties) {
        this.bigQueryService = bigQueryService;
        this.tradePath = new File(mercuriusProperties.getTradePath());
    }

    @SneakyThrows
    @PostConstruct
    public void execute() {
        List<AsbMercuriusTrade> asbMercuriusTrades = asbMercuriusTrades();
        updateFromMarketCatalogue(asbMercuriusTrades);
        updateFromHistoricalData(asbMercuriusTrades);
        updateFromPriceAnalytics(asbMercuriusTrades);
        updateInplayFeatures(asbMercuriusTrades);

        Writer writer = new FileWriter(new File(tradePath.getParentFile(), ASB_MERCURIUS_TRADE_FILE_NAME));
        StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
        beanToCsv.write(asbMercuriusTrades);
        writer.close();
    }

    private List<AsbMercuriusTrade> asbMercuriusTrades() throws FileNotFoundException {
        List<MercuriusTrade> mercuriusTrades = new CsvToBeanBuilder(new FileReader(tradePath))
                .withType(MercuriusTrade.class).build().parse();
        log.info("Mercurius has {} original trades", mercuriusTrades.size());
        mercuriusTrades = mercuriusTrades.stream().filter(mercuriusTrade -> mercuriusTrade.getAsianHandicap().isEmpty() || mercuriusTrade.getAsianHandicap().isBlank()).collect(Collectors.toList());

        log.info("Mercurius has {} filtered trades", mercuriusTrades.size());
        List<AsbMercuriusTrade> asbMercuriusTrades = new ArrayList<>();
        mercuriusTrades.forEach(mercuriusTrade -> {
            AsbMercuriusTrade asbMercuriusTrade = AsbMercuriusTrade.builder()
                    .amount(mercuriusTrade.getAmount())
                    .away(mercuriusTrade.getAway())
                    .executionStart(mercuriusTrade.getExecutionStart())
                    .executionEnd(mercuriusTrade.getExecutionEnd())
                    .eventStartAt(mercuriusTrade.getEventStartAt())
                    .home(mercuriusTrade.getHome())
                    .marketId(mercuriusTrade.getMarketId())
                    .selectionId(mercuriusTrade.getSelectionId())
                    .odds(mercuriusTrade.getOdds())
                    .build();
            asbMercuriusTrades.add(asbMercuriusTrade);
        });
        return asbMercuriusTrades;
    }

    private void updateInplayFeatures(List<AsbMercuriusTrade> asbMercuriusTrades) throws InterruptedException {
        String eventParams = asbMercuriusTrades.stream()
                .filter(asbMercuriusTrade -> asbMercuriusTrade.getEventId() != null)
                .map(asbMercuriusTrade -> String.format("'%s'", asbMercuriusTrade.getEventId()))
                .collect(Collectors.joining(","));

        Map<String, Boolean> eventInplayFeaturesCache = new HashMap<>();
        String sql = String.format(EVENT_INPLAY_FEATURES_SQL, eventParams);
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> result = bigQueryService.performQuery(sql);
        if (!isEmpty(result)) {
            result.forEach(row -> {
                String eventId = (String) row.get("eventId").orElse("");
                if (!eventId.isEmpty()) {
                    eventInplayFeaturesCache.putIfAbsent(eventId, true);
                }
            });
        }
        asbMercuriusTrades.forEach(asbMercuriusTrade -> {
            if( asbMercuriusTrade.getEventId() != null) {
                asbMercuriusTrade.setHasInplayFeatures(eventInplayFeaturesCache.containsKey(asbMercuriusTrade.getEventId()));
            }
        });
    }

    private void updateFromPriceAnalytics(List<AsbMercuriusTrade> asbMercuriusTrades) throws InterruptedException {
        String eventParams = asbMercuriusTrades.stream()
                .filter(asbMercuriusTrade -> asbMercuriusTrade.getEventId() != null)
                .map(asbMercuriusTrade -> String.format("'%s'", asbMercuriusTrade.getEventId()))
                .collect(Collectors.joining(","));
        Map<String, List<String>> event2AsbSelectionsPriceAnalyticsCache = new HashMap<>();
        String sql = String.format(PRICES_ANALYTICS_SQL, eventParams);
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> result = bigQueryService.performQuery(sql);
        if (!isEmpty(result)) {
            result.forEach(row -> {
                String eventId = (String) row.get("eventId").orElse("");
                String asbSelectionId = (String) row.get("asbSelectionId").orElse("");
                if (!eventId.isEmpty() && !asbSelectionId.isEmpty()) {
                    List<String> asbSelections = event2AsbSelectionsPriceAnalyticsCache.get(eventId);
                    if( asbSelections == null) {
                        asbSelections = new ArrayList<>();
                    }
                    asbSelections.add(asbSelectionId);
                    event2AsbSelectionsPriceAnalyticsCache.put(eventId, asbSelections);
                }
            });
        }
        asbMercuriusTrades.forEach(asbMercuriusTrade -> {
            String asbSelectionId = asbMercuriusTrade.getAsbSelectionId();
            if(asbMercuriusTrade.getEventId() != null) {
                List<String> asbSelections = event2AsbSelectionsPriceAnalyticsCache.get(asbMercuriusTrade.getEventId());
                asbMercuriusTrade.setHasPriceAnalytics(asbSelections != null && asbSelectionId != null && asbSelections.contains(asbSelectionId));
            }
        });
    }

    private void updateFromHistoricalData(List<AsbMercuriusTrade> asbMercuriusTrades) throws InterruptedException {
        Map<String, String> marketSelection2EventHistoricalDataCache = new HashMap<>();
        log.info("sql={}", HISTORICAL_DATA_SQL);
        List<Map<String, Optional<Object>>> result = bigQueryService.performQuery(HISTORICAL_DATA_SQL);
        if (!isEmpty(result)) {
            result.forEach(row -> {
                String marketId = (String) row.get("marketId").orElse("");
                String selectionId = (String) row.get("selectionId").orElse("");
                String asbSelectionId = (String) row.get("asbSelectionId").orElse("");
                if (!marketId.isEmpty() && !selectionId.isEmpty() && !asbSelectionId.isEmpty()) {
                    marketSelection2EventHistoricalDataCache.putIfAbsent(String.format("%s_%s", marketId, selectionId), asbSelectionId);
                }
            });
        }
        asbMercuriusTrades.forEach(asbMercuriusTrade -> {
            String marketSelectionId = String.format("%s_%s", asbMercuriusTrade.getMarketId(), asbMercuriusTrade.getSelectionId());
            asbMercuriusTrade.setHasHistoricalData(marketSelection2EventHistoricalDataCache.get(marketSelectionId) != null);
        });
    }

    private void updateFromMarketCatalogue(List<AsbMercuriusTrade> asbMercuriusTrades) throws InterruptedException {
        Map<String, String> marketSelection2EventCatalogueCache = new HashMap<>();
        Map<String, String> marketSelection2AsbSelectionCatalogueCache = new HashMap<>();

        log.info("sql={}", MARKET_CATALOGUE_SQL);
        List<Map<String, Optional<Object>>> result = bigQueryService.performQuery(MARKET_CATALOGUE_SQL);
        if (!isEmpty(result)) {
            result.forEach(row -> {
                String marketId = (String) row.get("marketId").orElse("");
                String selectionId = (String) row.get("selectionId").orElse("");
                String asbSelectionId = (String) row.get("asbSelectionId").orElse("");
                String eventId = (String) row.get("eventId").orElse("");
                if (!marketId.isEmpty() && !selectionId.isEmpty() && !asbSelectionId.isEmpty() && !eventId.isEmpty()) {
                    marketSelection2EventCatalogueCache.putIfAbsent(String.format("%s_%s", marketId, selectionId), eventId);
                    marketSelection2AsbSelectionCatalogueCache.putIfAbsent(String.format("%s_%s", marketId, selectionId), asbSelectionId);
                }
            });
        }
        asbMercuriusTrades.forEach(asbMercuriusTrade -> {
            String marketSelectionId = String.format("%s_%s", asbMercuriusTrade.getMarketId(), asbMercuriusTrade.getSelectionId());
            asbMercuriusTrade.setEventId(marketSelection2EventCatalogueCache.get(marketSelectionId));
            asbMercuriusTrade.setAsbSelectionId(marketSelection2AsbSelectionCatalogueCache.get(marketSelectionId));
        });
    }
}
