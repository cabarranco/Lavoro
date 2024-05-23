package com.asbresearch.collector.copy;

import com.asbresearch.collector.config.CopyProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.asbresearch.collector.util.CollectionUtils.isEmpty;
import static com.asbresearch.common.BigQueryUtil.PULSE_REPORTING_DATASET;

@Component("PulseReportingTablesCopy")
@EnableConfigurationProperties({CopyProperties.class})
@ConditionalOnProperty(prefix = "copy", name = "pulseReportingTablesCopy", havingValue = "on")
@AllArgsConstructor
@Slf4j
public class PulseReportingTablesCopy {
    private final static Map<String, List<String>> tableColumns = createTableColumns();

    private static Map<String, List<String>> createTableColumns() {
        return ImmutableMap.<String, List<String>>builder()
                .put("account_balance", Arrays.asList("datetime", "username", "availableToBet", "currency", "balanceSaving", "tradingDayAvailableBalance"))
                .put("audit", Arrays.asList("opportunityId", "marketChangeId", "strategyId", "marketId", "eventId", "logEntry", "logEntryTimestamp"))
                .put("orders_bets", Arrays.asList(
                        "orderTimestamp",
                        "venue",
                        "orderStatus",
                        "bookRunner",
                        "marketId",
                        "orderSide",
                        "orderAllocation",
                        "orderAllocationCurrency",
                        "orderPrice",
                        "orderType",
                        "betAmount",
                        "betAmountCurrency",
                        "betPrice",
                        "abortReason",
                        "betId",
                        "selectionId",
                        "eventId",
                        "opportunityId",
                        "strategyId",
                        "executionStatus",
                        "eventName",
                        "inPlay"))
//                .put("strategies", Arrays.asList("strategyId", "allocatorId", "hedgeStrategyId", "isActive", "json", "node"))
                .put("strategies_audit", Arrays.asList("time", "strategyId", "allocatorId", "hedgeStrategyId", "json", "node"))
                .build();
    }

    private final BigQueryService source;
    private final SecondaryBigQueryService destination;

    @PostConstruct
    public void copy() {
        log.info("Begin PulseReportingTablesCopy");
        tableColumns.forEach((tableName, columnNames) -> {
            try {
                String sql = String.format("select * from `%s.%s`", PULSE_REPORTING_DATASET, tableName);
                log.info("sql={}", sql);
                List<Map<String, Optional<Object>>> sourceRows = source.performQuery(sql);
                List<String> rowsToReplicate = sourceRows.stream()
                        .map(row -> columnNames.stream()
                                .map(columnName -> row.get(columnName).orElse("").toString())
                                .collect(Collectors.joining("|")))
                        .collect(Collectors.toList());
                if (!isEmpty(rowsToReplicate)) {
                    try {
                        destination.insertRows(PULSE_REPORTING_DATASET, tableName, rowsToReplicate);
                    } catch (RuntimeException ex) {
                        log.error("BigQuery error writing to {}", tableName, ex);
                    }
                }
            } catch (InterruptedException e) {
                log.error("Error occurred trying to copy {} details", tableName, e);
            } finally {
                log.info("End copy of {}", tableName);
            }
        });
        log.info("End PulseReportingTablesCopy");
    }
}
