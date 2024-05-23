package com.asbresearch.pulse.service.audit;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.util.ThreadUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.unix4j.Unix4j;
import org.unix4j.line.Line;

import static com.asbresearch.pulse.util.Constants.PULSE_REPORTING;

@Service
@EnableConfigurationProperties({AppProperties.class})
@Slf4j
public class LogEntryAuditService {
    private final BigQueryService bigQueryService;
    private final AppProperties appProperties;
    private final ExecutorService worker;

    @Autowired
    public LogEntryAuditService(BigQueryService bigQueryService,
                                AppProperties appProperties) {

        Preconditions.checkNotNull(bigQueryService, "bigQueryService must be provided");
        Preconditions.checkNotNull(appProperties, "appProperties must be provided");

        this.bigQueryService = bigQueryService;
        this.appProperties = appProperties;
        worker = Executors.newSingleThreadExecutor(ThreadUtils.threadFactoryBuilder("audit").build());
    }

    @PreDestroy
    public void shutDown() {
        if (worker != null) {
            worker.shutdown();
            try {
                worker.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for bigQuery service tasks to complete", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void uploadAuditAsync(String opportunityId) {
        CompletableFuture.runAsync(() -> uploadAudit(opportunityId), worker);
    }

    public void uploadAudit(String opportunityId) {
        if (appProperties.isLogAudit()) {
            try {
                String today = LocalDate.now().toString();
                List<File> files = Lists.newArrayList(Paths.get(appProperties.getLogDirectory(), "pulse.log").toFile());
                File archived = Paths.get(appProperties.getLogDirectory(), "archived").toFile();
                files.addAll(Arrays.stream(archived.listFiles((dir, name) -> name.contains(today))).collect(Collectors.toList()));

                List<Line> lines = new ArrayList<>();
                for (File file : files) {
                    lines.addAll(Unix4j.grep(String.format("op.id=%s", opportunityId), file).toLineList());
                }
                List<String> csvData = lines.stream()
                        .map(line -> LogEntry.of(line.getContent()))
                        .map(logEntry -> logEntry.toCsvData())
                        .collect(Collectors.toList());
                log.info("BigQuery Begin size={} lines for opportunityId={}", csvData.size(), opportunityId);
                bigQueryService.insertRows(PULSE_REPORTING, "audit", csvData);
                log.info("BigQuery End size={} lines for opportunityId={}", csvData.size(), opportunityId);
            } catch (RuntimeException rte) {
                log.error("Error while trying to upload log entry for opId={}", opportunityId, rte);
            }
        }
    }
}
