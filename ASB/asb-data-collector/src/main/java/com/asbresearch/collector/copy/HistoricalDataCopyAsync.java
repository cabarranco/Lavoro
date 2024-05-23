package com.asbresearch.collector.copy;

import com.asbresearch.collector.config.CopyProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import static com.asbresearch.collector.util.Constants.eventIdCol;
import static com.asbresearch.collector.util.Constants.publishTimeCol;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.BigQueryUtil.HISTORICAL_DATA_TABLE;
import static com.asbresearch.common.BigQueryUtil.RESEARCH_DATASET;
import static com.asbresearch.common.BigQueryUtil.csvValue;

@Component("HistoricalDataCopyAsync")
@EnableConfigurationProperties({CopyProperties.class})
@ConditionalOnProperty(prefix = "copy", name = "historicalDataCopy", havingValue = "on")
@Slf4j
public class HistoricalDataCopyAsync {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final BigQueryService source;
    private final SecondaryBigQueryService destination;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @Autowired
    public HistoricalDataCopyAsync(BigQueryService source,
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
            Map<String, Instant> copied = previouslyCopied(currentDate, nextDay);
            Instant start = Instant.now();
            try {
                log.info("Begin copyHistoricalData start={} end={}", currentDate, nextDay);
                String sql = String.format("select  eventId, marketId, asbSelectionId, selectionId, status, inplay, totalMatched, backPrice, backSize, " +
                                "layPrice, laySize, unix_millis(publishTime) as publishTime " +
                                "from `%s.betfair_historical_data_copy` where DATE(publishTime)  >= '%s' and DATE(publishTime) < '%s' ",
                        RESEARCH_DATASET,
                        currentDate.format(dateTimeFormatter),
                        nextDay.format(dateTimeFormatter));
                log.info("sql={}", sql);
                List<Map<String, Optional<Object>>> rows = source.performQuery(sql).stream().map(row -> {
                    Instant startTime = Instant.ofEpochMilli(Long.valueOf(row.get(publishTimeCol).get().toString()));
                    row.put(publishTimeCol, Optional.of(startTime));
                    return row;
                }).collect(Collectors.toList());
                log.info("End read historicalData from source");
                if (!copied.isEmpty()) {
                    rows = rows.stream().filter(row -> {
                        String copiedKey = String.format("%s.%s", row.get(eventIdCol).get().toString(), row.get(asbSelectionIdCol).get().toString());
                        Instant copiedPublishTime = copied.get(copiedKey);
                        if (copiedPublishTime != null) {
                            Instant currentPublishTime = (Instant) row.get(publishTimeCol).get();
                            if (currentPublishTime.isBefore(copiedPublishTime) || currentPublishTime.equals(copiedPublishTime)) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toList());
                }
                rows.stream().forEach(row -> {
                    String entry = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                            csvValue(row.get("eventId").orElse("")),
                            csvValue(row.get("marketId").orElse("")),
                            csvValue(row.get("asbSelectionId").orElse("")),
                            csvValue(row.get("selectionId").orElse("")),
                            csvValue(row.get("status").orElse("")),
                            csvValue(row.get("inplay").orElse("")),
                            csvValue(row.get("totalMatched").orElse("")),
                            csvValue(row.get("backPrice").orElse("")),
                            csvValue(row.get("backSize").orElse("")),
                            csvValue(row.get("layPrice").orElse("")),
                            csvValue(row.get("laySize").orElse("")),
                            csvValue(row.get("publishTime").orElse("")));
                    log.debug("Copying {}", entry);
                    destination.insertRows(BETSTORE_DATASET, HISTORICAL_DATA_TABLE, Arrays.asList(entry));
                });
            } catch (InterruptedException e) {
                throw new RuntimeException("Error occurred while trying copy historicalData", e);
            } finally {
                log.info("End copyHistoricalData took {}ms start={} end={}", Duration.between(start, Instant.now()).toMillis(), currentDate, nextDay);
            }
            currentDate = currentDate.plusDays(1);
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while sleeping");
                break;
            }
        }
    }

    private Map<String, Instant> previouslyCopied(LocalDate startDate, LocalDate endDate) {
        Map<String, Instant> result = new ConcurrentHashMap<>();
        Instant start = Instant.now();
        try {
            log.info("Begin previouslyCopied");
            String sql = String.format("SELECT eventId, asbSelectionId, unix_millis(publishTime) as publishTime FROM `%s.%s` where DATE(publishTime)  >= '%s' and DATE(publishTime) < '%s' order by eventId,  asbSelectionId, publishTime",
                    BETSTORE_DATASET,
                    HISTORICAL_DATA_TABLE,
                    startDate.format(dateTimeFormatter),
                    endDate.format(dateTimeFormatter));
            log.info("sql={}", sql);
            destination.performQuery(sql).parallelStream().forEach(row -> {
                Instant publishTime = Instant.ofEpochMilli(Long.valueOf(row.get(publishTimeCol).get().toString()));
                result.put(String.format("%s.%s", row.get(eventIdCol).get().toString(), row.get(asbSelectionIdCol).get().toString()), publishTime);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("Error occurred while trying to compute previouslyCopied historical data", e);
        } finally {
            log.info("End previouslyCopied took {}ms with {} rows", Duration.between(start, Instant.now()).toMillis(), result.size());
        }
        return result;
    }

}
