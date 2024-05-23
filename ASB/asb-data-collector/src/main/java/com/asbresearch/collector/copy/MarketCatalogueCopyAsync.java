package com.asbresearch.collector.copy;

import com.asbresearch.collector.config.CopyProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.asbresearch.collector.util.Constants.asbSelectionIdCol;
import static com.asbresearch.collector.util.Constants.dateTimeFormatter;
import static com.asbresearch.collector.util.Constants.eventIdCol;
import static com.asbresearch.collector.util.Constants.startTimeCol;
import static com.asbresearch.common.BigQueryUtil.BETFAIR_MARKET_CATALOGUE_TABLE;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.BigQueryUtil.csvValue;

@Component("MarketCatalogueCopyAsync")
@EnableConfigurationProperties({CopyProperties.class})
@ConditionalOnProperty(prefix = "copy", name = "marketCatalogueCopy", havingValue = "on")
@Slf4j
public class MarketCatalogueCopyAsync {
    private final BigQueryService source;
    private final SecondaryBigQueryService destination;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @Autowired
    public MarketCatalogueCopyAsync(BigQueryService source,
                                    SecondaryBigQueryService destination,
                                    CopyProperties copyProperties) {
        this.source = source;
        this.destination = destination;

        this.startDate = LocalDate.parse(copyProperties.getHistoricalStartDate(), dateTimeFormatter);
        this.endDate = LocalDate.parse(copyProperties.getHistoricalEndDate(), dateTimeFormatter);
    }

    @Async
    public void copy() {
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            LocalDate nextDay = currentDate.plusDays(1);
            Map<String, Boolean> copied = previouslyCopied(currentDate, nextDay);
            Instant start = Instant.now();
            try {
                log.info("Begin marketCatalogueCopy start={} end={}", currentDate, nextDay);
                String sql = String.format("select unix_millis(startTime) as startTime, competition, eventName, eventId, marketName, marketId, runnerName, selectionId, asbSelectionId, id " +
                                "from `%s.betfair_market_catalogue_part` where DATE(startTime)  >= '%s' and DATE(startTime) < '%s' ",
                        BETSTORE_DATASET,
                        currentDate.format(dateTimeFormatter),
                        nextDay.format(dateTimeFormatter));
                log.debug("sql={}", sql);
                List<Map<String, Optional<Object>>> rows = source.performQuery(sql).stream().map(row -> {
                    Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get(startTimeCol).get().toString()));
                    row.put(startTimeCol, Optional.of(startTime));
                    return row;
                }).collect(Collectors.toList());
                log.info("End read marketCatalogue from source");
                if (!copied.isEmpty()) {
                    rows = rows.stream().filter(row -> {
                        String copiedKey = String.format("%s_%s", row.get(eventIdCol).get().toString(), row.get(asbSelectionIdCol).get().toString());
                        return !copied.containsKey(copiedKey);
                    }).collect(Collectors.toList());
                }
                rows.stream().forEach(row -> {
                    String entry = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                            csvValue(row.get("startTime").orElse("")),
                            csvValue(row.get("competition").orElse("")),
                            csvValue(row.get("eventName").orElse("")),
                            csvValue(row.get("eventId").orElse("")),
                            csvValue(row.get("marketName").orElse("")),
                            csvValue(row.get("marketId").orElse("")),
                            csvValue(row.get("runnerName").orElse("")),
                            csvValue(row.get("selectionId").orElse("")),
                            csvValue(row.get("asbSelectionId").orElse("")),
                            csvValue(row.get("id").orElse("")));
                    log.debug("Copying {}", entry);
                    destination.insertRows(BETSTORE_DATASET, BETFAIR_MARKET_CATALOGUE_TABLE, Arrays.asList(entry));
                });
            } catch (InterruptedException e) {
                throw new RuntimeException("Error occurred while trying copy marketCatalogue", e);
            } finally {
                log.info("End marketCatalogueCopy took {}ms start={} end={}", Duration.between(start, Instant.now()).toMillis(), currentDate, nextDay);
            }
            currentDate = currentDate.plusDays(1);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while sleeping");
                break;
            }
        }
    }

    private Map<String, Boolean> previouslyCopied(LocalDate startDate, LocalDate endDate) {
        Map<String, Boolean> result = new ConcurrentHashMap<>();
        Instant start = Instant.now();
        try {
            log.info("Begin previouslyCopied");
            String sql = String.format("SELECT eventId, asbSelectionId FROM `%s.%s` where DATE(startTime)  >= '%s' and DATE(startTime) < '%s' order by eventId, asbSelectionId",
                    BETSTORE_DATASET,
                    BETFAIR_MARKET_CATALOGUE_TABLE,
                    startDate.format(dateTimeFormatter),
                    endDate.format(dateTimeFormatter));
            log.debug("sql={}", sql);
            destination.performQuery(sql).stream().forEach(row -> {
                String eventId = row.get(eventIdCol).get().toString();
                String asbSelectionId = row.get(asbSelectionIdCol).get().toString();
                result.put(String.format("%s_%s", eventId, asbSelectionId), true);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("Error occurred while trying to compute previouslyCopied marketCatalogue data", e);
        } finally {
            log.info("End previouslyCopied took {}ms with {} rows", Duration.between(start, Instant.now()).toMillis(), result.size());
        }
        return result;
    }

}
