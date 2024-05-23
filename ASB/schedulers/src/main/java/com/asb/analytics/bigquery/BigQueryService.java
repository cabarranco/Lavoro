package com.asb.analytics.bigquery;

import com.asb.analytics.util.ThreadUtils;
import com.google.cloud.bigquery.*;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.ZoneOffset.UTC;


public class BigQueryService {
    public static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(UTC);
    private final BigQuery bigQuery;
    private final BigQueryProperties bigQueryProperties;
    private final ExecutorService worker;

    public BigQueryService(BigQuery bigQuery,
                           BigQueryProperties bigQueryProperties) {

        checkNotNull(bigQuery, "bigQuery must be provided");
        checkNotNull(bigQueryProperties, "bigQueryProperties must be provided");

        this.bigQuery = bigQuery;
        this.bigQueryProperties = bigQueryProperties;
        worker = Executors.newFixedThreadPool(bigQueryProperties.getThreads(), ThreadUtils.threadFactoryBuilder("bqs").build());
    }

    public void insertRowsAsync(String tableName, List<String> csvRows) {
        Preconditions.checkNotNull(tableName, "tableName must be provided");
        Preconditions.checkNotNull(csvRows, "csvRows must be provided");

        CompletableFuture.runAsync(() -> insertRows(tableName, csvRows), worker);
    }

    public long insertRows(String tableName, List<String> csvRows) {
        Preconditions.checkNotNull(tableName, "tableName must be provided");
        Preconditions.checkNotNull(csvRows, "csvRows must be provided");

        long totalRows = 0;
        if (!csvRows.isEmpty()) {
            TableId tableId = TableId.of(bigQueryProperties.getProjectId(), bigQueryProperties.getDatasetName(), tableName);
            WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration.newBuilder(tableId).setFormatOptions(FormatOptions.csv()).build();
            TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration);
            try {
                writer.write(toByteBuffer(csvRows));
            } catch (IOException e) {
                System.out.println("Error writing account balance record to bigQuery: " + e.getMessage());
                if (bigQueryProperties.isNotificationOn()) {
                    String message = String.format("Error writing bigQuery rows=%s table=%s rootCause=%s", csvRows.size(), tableId, e.toString());
                }
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Error closing bigQuery writer: " + e.getMessage());
                }
            }
            Job job = writer.getJob();
            if (job == null) {
                throw new RuntimeException("Job no longer exists");
            }
            try {
                job = job.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while writing records to bigQuery", e);
            }
            if (job.getStatus().getError() != null) {
                throw new RuntimeException(String.format("bigQuery insert row error=%s", job.getStatus().getError()));
            }
            JobStatistics.LoadStatistics stats = job.getStatistics();
//            log.info("BigQuery rows={} inserted into table={}.{}.{}", stats.getOutputRows(), tableId.getProject(), tableId.getDataset(), tableId.getTable());
            totalRows = stats.getOutputRows();
        }
        return totalRows;
    }

    protected ByteBuffer toByteBuffer(List<String> csvRows) {
        String merged = csvRows.stream().collect(Collectors.joining("\n"));
        return ByteBuffer.wrap(merged.getBytes(Charsets.UTF_8));
    }

    @PreDestroy
    public void shutDown() {
        if (worker != null) {
            worker.shutdownNow();
        }
    }

    public List<Map<String, Optional<Object>>> performQuery(String query) throws InterruptedException {
        TableResult tableResult = bigQuery.query(QueryJobConfiguration.newBuilder(query).build());
        Schema schema = tableResult.getSchema();
        List<Map<String, Optional<Object>>> result = new LinkedList<>();
        for (FieldValueList rows : tableResult.iterateAll()) {
            Map<String, Optional<Object>> row = new HashMap<>();
            int counter = 0;
            for (FieldValue fieldValue : rows) {
                row.put(schema.getFields().get(counter++).getName(), fieldValue.getValue() != null ? Optional.of(fieldValue.getValue()) : Optional.empty());
            }
            result.add(row);
        }
        return result;
    }


}
