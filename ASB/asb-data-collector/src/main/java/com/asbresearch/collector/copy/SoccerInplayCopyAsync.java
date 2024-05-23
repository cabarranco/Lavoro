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

import static com.asbresearch.collector.util.Constants.dateTimeFormatter;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.BigQueryUtil.SOCCER_INPLAY_TABLE;
import static com.asbresearch.common.BigQueryUtil.csvValue;

@Component("SoccerInplayCopyAsync")
@EnableConfigurationProperties({CopyProperties.class})
@ConditionalOnProperty(prefix = "copy", name = "soccerInplayCopy", havingValue = "on")
@Slf4j
public class SoccerInplayCopyAsync {
    private final BigQueryService source;
    private final SecondaryBigQueryService destination;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @Autowired
    public SoccerInplayCopyAsync(BigQueryService source,
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
                log.info("Begin soccerInplayCopy");
                String sql = String.format("select id, eventId, unix_millis(updateTime) as updateTime, matchTime, team, updateType, score from `%s.betfair_soccer_inplay` " +
                                " where eventId in (select  distinct eventId from `%s.betfair_market_catalogue` where DATE(startTime) >= '%s' and DATE(startTime) < '%s')",
                        BETSTORE_DATASET,
                        BETSTORE_DATASET,
                        currentDate.format(dateTimeFormatter),
                        nextDay.format(dateTimeFormatter));
                log.debug("sql={}", sql);
                List<Map<String, Optional<Object>>> rows = source.performQuery(sql).stream().map(row -> {
                    Instant updateTime = Instant.ofEpochMilli(Long.valueOf(row.get("updateTime").get().toString()));
                    row.put("updateTime", Optional.of(updateTime));
                    return row;
                }).collect(Collectors.toList());
                log.info("End read soccerInplay from source");
                if (!copied.isEmpty()) {
                    rows = rows.stream().filter(row -> !copied.containsKey(row.get("id").get().toString())).collect(Collectors.toList());
                }
                rows.stream().forEach(row -> {
                    String entry = String.format("%s|%s|%s|%s|%s|%s|%s",
                            csvValue(row.get("id").orElse("")),
                            csvValue(row.get("eventId").orElse("")),
                            csvValue(row.get("updateTime").orElse("")),
                            csvValue(row.get("matchTime").orElse("")),
                            csvValue(row.get("team").orElse("")),
                            csvValue(row.get("updateType").orElse("")),
                            csvValue(row.get("score").orElse("")));
                    log.debug("Copying {}", entry);
                    destination.insertRows(BETSTORE_DATASET, SOCCER_INPLAY_TABLE, Arrays.asList(entry));
                });
            } catch (InterruptedException e) {
                throw new RuntimeException("Error occurred while trying copy soccerInplay", e);
            } finally {
                log.info("End soccerInplayCopy took {}ms start={} end={}", Duration.between(start, Instant.now()).toMillis(), currentDate, nextDay);
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
            String sql = String.format("SELECT id FROM `%s.betfair_soccer_inplay` where eventId in (select  distinct eventId from `%s.betfair_market_catalogue` where DATE(startTime) >= '%s' and DATE(startTime) < '%s') ",
                    BETSTORE_DATASET,
                    BETSTORE_DATASET,
                    startDate.format(dateTimeFormatter),
                    endDate.format(dateTimeFormatter));
            log.debug("sql={}", sql);
            destination.performQuery(sql).forEach(row -> result.put(row.get("id").get().toString(), true));
        } catch (InterruptedException e) {
            throw new RuntimeException("Error occurred while trying to compute previouslyCopied SoccerInplay data", e);
        } finally {
            log.info("End previouslyCopied took {}ms with {} rows", Duration.between(start, Instant.now()).toMillis(), result.size());
        }
        return result;
    }

}
