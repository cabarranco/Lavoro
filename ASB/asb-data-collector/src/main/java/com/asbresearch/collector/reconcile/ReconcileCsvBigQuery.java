package com.asbresearch.collector.reconcile;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.asbresearch.collector.util.CollectionUtils.isEmpty;
import static com.asbresearch.collector.util.Constants.*;

@Component("ReconcileCsvBigQuery")
@Slf4j
@EnableConfigurationProperties({BigQueryProperties.class, EmailProperties.class, CollectorProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "csvFileToBigQueryReconcile", havingValue = "on")
public class ReconcileCsvBigQuery {
    private final static Pattern fullTableNamePattern = Pattern.compile("^([^.]+)[.]([^.]+)$");
    private final BigQueryProperties bigQueryProperties;
    private final EmailProperties emailProperties;
    private final EmailNotifier emailNotifier;
    private final CollectorProperties collectorProperties;
    private final BigQueryService bigQueryService;

    @Autowired
    public ReconcileCsvBigQuery(BigQueryProperties bigQueryProperties,
                                EmailProperties emailProperties,
                                EmailNotifier emailNotifier,
                                CollectorProperties collectorProperties,
                                BigQueryService bigQueryService) {
        this.bigQueryProperties = bigQueryProperties;
        this.emailProperties = emailProperties;
        this.emailNotifier = emailNotifier;
        this.collectorProperties = collectorProperties;
        this.bigQueryService = bigQueryService;
    }

    @Scheduled(cron = "${collector.reconcileCsvBigQuery.cronExpression:0 0 0 * * *}")
    public void execute() {
        LocalDate startDate = LocalDate.parse(startDate(collectorProperties), dateTimeFormatter);
        LocalDate endDate = LocalDate.parse(endDate(collectorProperties), dateTimeFormatter);
        Set<String> tables = listTables();
        if (!isEmpty(collectorProperties.getIgnoreDataReconcile())) {
            tables = tables.stream()
                    .filter(table -> !collectorProperties.getIgnoreDataReconcile().contains(table)).collect(Collectors.toSet());
        }
        if (tables.isEmpty()) {
            emailNotifier.sendMessage(String.format("No tables found in dir=%s", bigQueryProperties.getDataDir().getAbsolutePath()), "ReconcileCsvBigQuery:Warning No tables in dataDir", emailProperties.getTo());
            return;
        }
        tables.forEach(fullTableName -> {
            Matcher matcher = fullTableNamePattern.matcher(fullTableName);
            if (matcher.find()) {
                String dataSet = matcher.group(1);
                String table = matcher.group(2);
                for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                    Set<File> copiedFiles = copiedFiles(fullTableName, date);
                    int totalCsvRowCount = copiedFiles.stream().map(file -> {
                        try (Stream<String> fileStream = Files.lines(file.toPath())) {
                            int noOfLines = (int) fileStream.count();
                            log.debug("file={} has lines={}", file.getAbsolutePath(), noOfLines);
                            return noOfLines;
                        } catch (IOException e) {
                            log.error("Error occurred trying to get line count for file={}", file.getAbsolutePath(), e);
                            return 0;
                        }
                    }).mapToInt(value -> value).sum();
                    int bigQueryTableRowCount = totalCsvRowCount > 0 ? bigQueryTableRowCount(dataSet, table, date) : 0;
                    if (totalCsvRowCount != bigQueryTableRowCount) {
                        String message = String.format("Row count mismatch totalCsvRowCount=%s bigQueryTableRowCount=%s fullTableName=%s startDate=%s endDate=%s",
                                totalCsvRowCount,
                                bigQueryTableRowCount,
                                fullTableName,
                                startDate,
                                endDate);
                        log.error("{}", message);
                        emailNotifier.sendMessage(message, String.format("ReconcileCsvBigQuery:Warning for table=%s", fullTableName), emailProperties.getTo());
                    }
                    log.info("fullTableName={} date={} totalCsvRowCount={} bigQueryTableRowCount={}", fullTableName, startDate, totalCsvRowCount, bigQueryTableRowCount);
                }
            } else {
                log.error("fullTableName={} not in expected format <dataset>.<table_name>", fullTableName);
            }
        });
    }

    private int bigQueryTableRowCount(String dataset, String table, LocalDate date) {
        String sql = String.format("SELECT count(*) as totalCount FROM `%s.%s` WHERE DATE(createTimestamp) ='%s'", dataset, table, date.format(dateTimeFormatter));
        if (!isEmpty(collectorProperties.getPartitionTables())) {
            String partitionColumn = collectorProperties.getPartitionTables().get(table);
            if (partitionColumn != null) {
                sql = String.format("%s AND DATE(%s) <= current_date() and  DATE(%s) >= DATE_SUB(current_date(), INTERVAL 3 DAY)", sql, partitionColumn, partitionColumn);
            }
        }
        log.info("sql={}", sql);
        try {
            List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
            if (!resultSet.isEmpty()) {
                Optional<Object> totalCount = resultSet.get(0).get("totalCount");
                if (totalCount.isPresent()) {
                    return Integer.parseInt(totalCount.get().toString());
                }
            }
        } catch (InterruptedException e) {
            log.error("Error occurred trying to execute sql={}", sql, e);
        }
        return 0;
    }

    private Set<File> copiedFiles(String fullTableName, LocalDate date) {
        File dir = new File(new File(bigQueryProperties.getDataDir(), fullTableName), date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        try (Stream<Path> stream = Files.list(dir.toPath())) {
            return stream.filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().endsWith("copied.csv"))
                    .map(Path::toFile)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Error listing csv files in dir={} fullTableName={} date={}", bigQueryProperties.getDataDir(), fullTableName, date, e);
            return Collections.emptySet();
        }
    }

    private Set<String> listTables() {
        try (Stream<Path> stream = Files.list(bigQueryProperties.getDataDir().toPath())) {
            return stream.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Error listing table names in dir={}", bigQueryProperties.getDataDir(), e);
            return Collections.emptySet();
        }
    }
}
