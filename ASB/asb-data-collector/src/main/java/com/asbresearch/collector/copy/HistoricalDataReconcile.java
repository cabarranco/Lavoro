package com.asbresearch.collector.copy;

import com.asbresearch.collector.config.CopyProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import java.time.LocalDate;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.asbresearch.collector.util.Constants.dateTimeFormatter;

@Component("HistoricalDataReconcile")
@EnableConfigurationProperties({CopyProperties.class})
@ConditionalOnProperty(prefix = "copy", name = "historicalDataReconcile", havingValue = "on")
@Slf4j
public class HistoricalDataReconcile {
    private final BigQueryService source;
    private final SecondaryBigQueryService destination;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @Autowired
    public HistoricalDataReconcile(BigQueryService source,
                                   SecondaryBigQueryService destination,
                                   CopyProperties copyProperties) {
        this.source = source;
        this.destination = destination;

        this.startDate = LocalDate.parse(copyProperties.getHistoricalStartDate(), dateTimeFormatter);
        this.endDate = LocalDate.parse(copyProperties.getHistoricalEndDate(), dateTimeFormatter);
    }

    @PostConstruct
    public void reconcile() {
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            LocalDate nextDay = currentDate.plusDays(1);
            try {
                String sql = String.format("SELECT count(*) as total FROM `asbresearch-prod.betstore.betfair_historical_data` where DATE(publishTime) >= '%s' and DATE(publishTime) < '%s'",
                        currentDate.format(dateTimeFormatter),
                        nextDay.format(dateTimeFormatter));
                log.debug("sql={}", sql);
                Optional<Integer> prodTotal = destination.performQuery(sql).stream().map(row -> Integer.valueOf(row.get("total").get().toString())).findFirst();
                sql = String.format("SELECT count(*) as total FROM `research.betfair_historical_data_copy` where DATE(publishTime) >= '%s' and DATE(publishTime) < '%s'",
                        currentDate.format(dateTimeFormatter),
                        nextDay.format(dateTimeFormatter));
                log.debug("sql={}", sql);
                Optional<Integer> oldTotal = source.performQuery(sql).stream().map(row -> Integer.valueOf(row.get("total").get().toString())).findFirst();
                if (!prodTotal.get().equals(oldTotal.get())) {
                    log.warn("Reconcile error current={} prev={} startDate={} endDate={}", prodTotal.get(), oldTotal.get(), currentDate, nextDay);
                }
                log.info("End reconcile for start={} end={}", currentDate, nextDay);
            } catch (InterruptedException e) {
                throw new RuntimeException("Error occurred while trying copy historicalData", e);
            }
            currentDate = currentDate.plusDays(1);
        }
    }
}
