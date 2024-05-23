package com.asbresearch.collector.derived;

import com.asbresearch.collector.betfair.EventTransactionDb;
import com.asbresearch.collector.betfair.model.BetfairHistoricalRecord;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.model.EventAsbselectionIdPair;
import com.asbresearch.collector.model.PriceAnalytics;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.betfair.esa.swagger.model.MarketDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.Precision;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.asbresearch.collector.util.CollectionUtils.isEmpty;
import static com.asbresearch.collector.util.Constants.*;
import static com.asbresearch.common.BigQueryUtil.EVENT_PRICES_ANALYTICS_TABLE;
import static com.asbresearch.common.BigQueryUtil.RESEARCH_DATASET;
import static java.time.temporal.ChronoUnit.*;

@Component
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class, BigQueryProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "eventPricesAnalyticsCalc", havingValue = "on")
@DependsOn({"SofaScoreSoccerInPlayEventDetails"})
public class EventPricesAnalyticsCalc {
    private final Map<EventAsbselectionIdPair, LinkedList<PriceAnalytics>> priceAnalytics = new ConcurrentHashMap<>();
    private final AtomicInteger insertedRows = new AtomicInteger(0);
    private final BigQueryService bigQueryService;
    private final AnalyticEventsProvider analyticEventsProvider;
    private final String startDateStr;
    private final String endDateStr;
    private final EventTransactionDb eventTransactionDb;
    private final Map<EventAsbselectionIdPair, Instant> latestAnalyticsTimestamp;
    private final CollectorProperties collectorProperties;

    public EventPricesAnalyticsCalc(BigQueryService bigQueryService,
                                    CollectorProperties collectorProperties,
                                    AnalyticEventsProvider analyticEventsProvider,
                                    EventTransactionDb eventTransactionDb) {
        this.collectorProperties = collectorProperties;
        this.bigQueryService = bigQueryService;
        this.analyticEventsProvider = analyticEventsProvider;
        startDateStr = startDate(collectorProperties);
        endDateStr = endDate(collectorProperties);
        this.eventTransactionDb = eventTransactionDb;
        latestAnalyticsTimestamp = latestAnalyticsTimestamp();
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    public void calcPricesAnalytics() {
        log.info("Begin calcPricesAnalytics startDate={} endDate={}", startDateStr, endDateStr);
        try {
            List<String> pendingEvents = new ArrayList<>(analyticEventsProvider.eventPeriods().keySet());
            while (!eventTransactionDb.isDbAvailable()) {
                log.info("Waiting for eventTransactionDb to be ready...sleeping for 1min");
                TimeUnit.MINUTES.sleep(1);
            }
            pendingEvents.parallelStream().forEach(eventId -> updateEventPricesAnalytics(eventId));
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error occurred with processing calcPricesAnalytics", e);
        } finally {
            log.info("End calcPricesAnalytics with totalRows={} for startDate={} endDate={}", insertedRows.get(), startDateStr, endDateStr);
        }
    }

    private Map<EventAsbselectionIdPair, Instant> latestAnalyticsTimestamp() {
        Map<EventAsbselectionIdPair, Instant> result = new ConcurrentHashMap<>();
        String sql = String.format("SELECT eventId, asbSelectionId, unix_millis(max(timestamp)) as lastTs FROM `research.event_prices_analytics` where timestamp between  '%s 04:00:00 UTC' and '%s 04:00:00 UTC' AND eventId in (%s) group by eventId, asbSelectionId  order by eventId, asbSelectionId",
                startDateStr, endDateStr, AnalyticEventsProvider.eventSql(collectorProperties));
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> currentAnalytics = Collections.emptyList();
        try {
            currentAnalytics = bigQueryService.performQuery(sql);
        } catch (InterruptedException e) {
            log.error("Error getting current event prices analytics", e);
        }
        currentAnalytics.forEach(row -> {
            String eventId = row.get(eventIdCol).get().toString();
            String asbSelectionId = row.get(asbSelectionIdCol).get().toString();
            Instant lastTs = Instant.ofEpochMilli(Long.parseLong(row.get("lastTs").get().toString()));
            result.put(new EventAsbselectionIdPair(eventId, asbSelectionId), lastTs);
        });
        return result;
    }

    private void updateEventPricesAnalytics(String eventId) {
        Map<String, EventPeriod> eventPeriods = analyticEventsProvider.eventPeriods();
        List<BetfairHistoricalRecord> historicalDataRows = loadHistoricalData(eventId);
        Set<String> eventAsbSelections = historicalDataRows.stream()
                .map(BetfairHistoricalRecord::getAsbSelectionId)
                .collect(Collectors.toSet());
        EventPeriod eventPeriod = eventPeriods.get(eventId);
        Instant currentTimeStamp = eventPeriod.getKickOffTime().minus(4, HOURS);
        Instant cutOff = eventPeriod.getSecondHalfEndTime();
        if (currentTimeStamp.isAfter(cutOff)) {
            currentTimeStamp = cutOff;
        }
        List<Optional<PriceAnalytics>> analytics = new ArrayList<>();
        while (currentTimeStamp.isBefore(cutOff) || currentTimeStamp.equals(cutOff)) {
            analytics.addAll(computeEventPricesAnalytics(historicalDataRows, eventId, currentTimeStamp, eventAsbSelections, latestAnalyticsTimestamp));
            if (currentTimeStamp.equals(cutOff)) {
                break;
            }
            currentTimeStamp = currentTimeStamp.plus(30, SECONDS);
            if (currentTimeStamp.isAfter(cutOff)) {
                currentTimeStamp = cutOff;
            }
        }
        List<String> csvRows = analytics.stream().filter(Optional::isPresent).map(opt -> opt.get().toString()).collect(Collectors.toList());
        bigQueryService.insertRows(RESEARCH_DATASET, EVENT_PRICES_ANALYTICS_TABLE, csvRows);
        insertedRows.addAndGet(csvRows.size());
        log.debug("End inserted updateEventPricesAnalytics for eventId={}", eventId);
        historicalDataRows.remove(eventId);
    }

    private List<Optional<PriceAnalytics>> computeEventPricesAnalytics(List<BetfairHistoricalRecord> eventHistoricalData,
                                                                       String eventId,
                                                                       Instant currentTimeStamp,
                                                                       Set<String> asbSelections,
                                                                       Map<EventAsbselectionIdPair, Instant> latestAnalyticsTimestamp) {
        List<Optional<PriceAnalytics>> result = Collections.emptyList();
        if (!isEmpty(eventHistoricalData) && !isEmpty(asbSelections)) {
            result = asbSelections.stream()
                    .map(asbSelection -> computeEventAsbSelectionAnalytics(eventHistoricalData, eventId, currentTimeStamp, latestAnalyticsTimestamp, asbSelection))
                    .collect(Collectors.toList());
        }
        return result;
    }

    private Optional<PriceAnalytics> computeEventAsbSelectionAnalytics(List<BetfairHistoricalRecord> eventHistoricalData, String eventId, Instant currentTimeStamp, Map<EventAsbselectionIdPair, Instant> latestAnalyticsTimestamp, String asbSelection) {
        EventAsbselectionIdPair eventSelectionKey = new EventAsbselectionIdPair(eventId, asbSelection);
        Instant lastKnownTs = latestAnalyticsTimestamp.get(eventSelectionKey);
        if (lastKnownTs == null || lastKnownTs.isBefore(currentTimeStamp)) {
            List<BetfairHistoricalRecord> tsMatch = eventHistoricalData.stream().filter(row -> filterByPublishTime(currentTimeStamp, row)).collect(Collectors.toList());
            priceAnalytics.putIfAbsent(eventSelectionKey, new LinkedList<>());
            Optional<BetfairHistoricalRecord> nearestBetOpt = tsMatch.stream()
                    .filter(row -> asbSelection.equals(row.getAsbSelectionId()))
                    .findFirst();
            if (nearestBetOpt.isPresent()) {
                BetfairHistoricalRecord nearestBet = nearestBetOpt.get();
                PriceAnalytics.PriceAnalyticsBuilder builder = PriceAnalytics.builder()
                        .eventId(eventId)
                        .timestamp(currentTimeStamp)
                        .asbSelectionId(asbSelection);
                Double layPrice = null;
                Double backPrice = null;
                Double backSize = null;
                Double laySize = null;
                Double spreadPrice = null;
                if (nearestBet.getRunnerPrice() != null && nearestBet.getRunnerPrice().getLayPrice() != null) {
                    layPrice = nearestBet.getRunnerPrice().getLayPrice();
                    builder.layPrice(layPrice);
                }
                if (nearestBet.getRunnerPrice() != null && nearestBet.getRunnerPrice().getLaySize() != null) {
                    laySize = nearestBet.getRunnerPrice().getLaySize();
                    builder.laySize(laySize);
                }
                if (nearestBet.getRunnerPrice() != null && nearestBet.getRunnerPrice().getBackPrice() != null) {
                    backPrice = nearestBet.getRunnerPrice().getBackPrice();
                    builder.backPrice(backPrice);
                }
                if (nearestBet.getRunnerPrice() != null && nearestBet.getRunnerPrice().getBackSize() != null) {
                    backSize = nearestBet.getRunnerPrice().getBackSize();
                    builder.backSize(backSize);
                }
                if (layPrice != null && backPrice != null) {
                    spreadPrice = Precision.round(layPrice - backPrice, 2);
                    builder.spreadPrice(spreadPrice);
                }
                LinkedList<PriceAnalytics> priceAnalyticsPerSelections = priceAnalytics.get(eventSelectionKey);
                Instant cutoff = currentTimeStamp.minus(15, MINUTES);
                priceAnalyticsPerSelections.removeIf(elem -> elem.getTimestamp().isBefore(cutoff));
                if (!priceAnalyticsPerSelections.isEmpty()) {
                    StandardDeviation stdDeviation = new StandardDeviation();
                    PriceAnalytics prevAnalytics = priceAnalyticsPerSelections.getFirst();
                    builder.deltaBackPrice(Precision.round(backPrice - prevAnalytics.getBackPrice(), 2));
                    builder.deltaLayPrice(Precision.round(layPrice - prevAnalytics.getLayPrice(), 2));
                    builder.deltaBackSize(Precision.round(backSize - prevAnalytics.getBackSize(), 2));
                    builder.deltaLaySize(Precision.round(laySize - prevAnalytics.getLaySize(), 2));
                    builder.deltaSpreadPrice(Precision.round(spreadPrice - prevAnalytics.getSpreadPrice(), 2));

                    List<PriceAnalytics> muAnalytics = priceAnalyticsPerSelections.stream()
                            .filter(item -> item.getTimestamp().isAfter(cutoff) || item.getTimestamp().equals(cutoff))
                            .collect(Collectors.toList());

                    List<Double> backPrices = muAnalytics.stream().map(PriceAnalytics::getBackPrice).collect(Collectors.toList());
                    backPrices.add(backPrice);
                    builder.muBackPrice(Precision.round(backPrices.stream().mapToDouble(Double::doubleValue).average().getAsDouble(), 2));
                    builder.sigmaBackPrice(Precision.round(stdDeviation.evaluate(backPrices.stream().mapToDouble(Double::doubleValue).toArray()), 2));

                    List<Double> layPrices = muAnalytics.stream().map(PriceAnalytics::getLayPrice).collect(Collectors.toList());
                    layPrices.add(layPrice);
                    builder.muLayPrice(Precision.round(layPrices.stream().mapToDouble(Double::doubleValue).average().getAsDouble(), 2));
                    builder.sigmaLayPrice(Precision.round(stdDeviation.evaluate(layPrices.stream().mapToDouble(Double::doubleValue).toArray()), 2));

                    List<Double> backSizes = muAnalytics.stream().map(PriceAnalytics::getBackSize).collect(Collectors.toList());
                    backSizes.add(backSize);
                    builder.muBackSize(Precision.round(backSizes.stream().mapToDouble(Double::doubleValue).average().getAsDouble(), 2));
                    builder.sigmaBackSize(Precision.round(stdDeviation.evaluate(backSizes.stream().mapToDouble(Double::doubleValue).toArray()), 2));

                    List<Double> laySizes = muAnalytics.stream().map(PriceAnalytics::getLaySize).collect(Collectors.toList());
                    laySizes.add(laySize);
                    builder.muLaySize(Precision.round(laySizes.stream().mapToDouble(Double::doubleValue).average().getAsDouble(), 2));
                    builder.sigmaLaySize(Precision.round(stdDeviation.evaluate(laySizes.stream().mapToDouble(Double::doubleValue).toArray()), 2));

                    List<Double> spreadPrices = muAnalytics.stream().map(item -> item.getSpreadPrice()).collect(Collectors.toList());
                    spreadPrices.add(spreadPrice);
                    builder.muSpreadPrice(Precision.round(spreadPrices.stream().mapToDouble(Double::doubleValue).average().getAsDouble(), 2));
                    builder.sigmaSpreadPrice(Precision.round(stdDeviation.evaluate(spreadPrices.stream().mapToDouble(Double::doubleValue).toArray()), 2));

                } else {
                    if (backPrice != null) {
                        builder.deltaBackPrice(0.0);
                        builder.muBackPrice(backPrice);
                        builder.sigmaBackPrice(0.0);
                    }
                    if (layPrice != null) {
                        builder.deltaLayPrice(0.0);
                        builder.muLayPrice(layPrice);
                        builder.sigmaLayPrice(0.0);
                    }
                    if (backSize != null) {
                        builder.deltaBackSize(0.0);
                        builder.muBackSize(backSize);
                        builder.sigmaBackSize(0.0);
                    }
                    if (laySize != null) {
                        builder.deltaLaySize(0.0);
                        builder.muLaySize(laySize);
                        builder.sigmaLaySize(0.0);
                    }
                    if (spreadPrice != null) {
                        builder.deltaSpreadPrice(0.0);
                        builder.muSpreadPrice(spreadPrice);
                        builder.sigmaSpreadPrice(0.0);
                    }
                }
                PriceAnalytics analytics = builder.build();
                priceAnalyticsPerSelections.addFirst(analytics);
                log.debug("analytics={}", analytics);
                return Optional.of(analytics);
            }
        }
        return Optional.empty();
    }

    private boolean filterByPublishTime(Instant timeStamp, BetfairHistoricalRecord row) {
        if (row.getPublishTime() != null) {
            long publishTime = row.getPublishTime().toEpochMilli();
            return publishTime <= timeStamp.toEpochMilli() && publishTime > timeStamp.minus(30, MINUTES).toEpochMilli();
        }
        return false;
    }

    private List<BetfairHistoricalRecord> loadHistoricalData(String eventId) {
        return eventTransactionDb.getTransactions(eventId).stream()
                .filter(betfairHistoricalRecord -> MarketDefinition.StatusEnum.OPEN == betfairHistoricalRecord.getStatus())
                .collect(Collectors.toList());
    }
}
