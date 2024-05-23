package com.asbresearch.common.bigquery;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.ThreadUtils;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.google.cloud.bigquery.*;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.asbresearch.common.BigQueryUtil.TEMP_DATASET;
import static com.asbresearch.common.BigQueryUtil.shortUUID;
import static com.google.cloud.bigquery.WriteChannelConfiguration.newBuilder;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.writeLines;

@Service
@EnableConfigurationProperties({BigQueryProperties.class, EmailProperties.class})
@Slf4j
public class BigQueryService {
    private final BigQuery bigQuery;
    private final BigQueryProperties bigQueryProperties;
    private final EmailProperties emailProperties;
    private final EmailNotifier emailNotifier;
    private final ScheduledExecutorService filerScheduler = Executors.newSingleThreadScheduledExecutor(ThreadUtils.threadFactoryBuilder("bq-filer").build());
    private final ExecutorService loaderScheduler = Executors.newSingleThreadExecutor(ThreadUtils.threadFactoryBuilder("bq-loader").build());
    private final Map<String, AtomicLong> countersPerTable = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, String>> dataPerTable = new ConcurrentHashMap<>();
    private final CsvOptions formatOptions;
    private final DB mapdbForBigQueryRecords;

    @Autowired
    public BigQueryService(@Qualifier("primary") BigQuery bigQuery,
                           BigQueryProperties bigQueryProperties,
                           EmailProperties emailProperties,
                           EmailNotifier emailNotifier) {

        checkNotNull(bigQuery, "bigQuery must be provided");
        checkNotNull(bigQueryProperties, "bigQueryProperties must be provided");
        checkNotNull(emailProperties, "emailProperties must be provided");
        checkNotNull(emailNotifier, "emailNotifier must be provided");

        this.bigQuery = bigQuery;
        this.bigQueryProperties = bigQueryProperties;
        this.emailProperties = emailProperties;
        this.emailNotifier = emailNotifier;
        formatOptions = CsvOptions.newBuilder().setFieldDelimiter(bigQueryProperties.getFieldDelimiter()).build();
        filerScheduler.scheduleWithFixedDelay(new BigQueryFilerTask(), 0, 1, TimeUnit.MINUTES);
        mapdbForBigQueryRecords = createMapdbForBigQueryRecords();
    }

    private DB createMapdbForBigQueryRecords() {
        File dbFile = new File(new File(bigQueryProperties.getDataDir(), "mapdb"), BASIC_ISO_DATE.format(LocalDate.now()));
        if (!dbFile.exists()) {
            dbFile.mkdirs();
        }
        return DBMaker.fileDB(new File(dbFile, "bq_tables.db"))
                .closeOnJvmShutdown()
                .fileDeleteAfterClose()
                .make();
    }

    private void createFile(String fullTableName, List<String> csvRows) {
        try {
            Map<File, List<String>> path2csvRows = path2csvRows(fullTableName, csvRows);
            for (File file : path2csvRows.keySet()) {
                writeLines(file, path2csvRows.get(file));
                log.info("BigQuery created file={}", file.getAbsolutePath());
                loaderScheduler.execute(new BigQueryLoaderTask(fullTableName, file));
            }
        } catch (RuntimeException | IOException e) {
            log.error("Error occurred trying to create csv file for fullTableName={}", fullTableName, e);
        }
    }

    private Map<File, List<String>> path2csvRows(String fullTableName, List<String> csvRows) {
        Integer createdDateIndex = bigQueryProperties.getCreatedDateTableMappings().get(fullTableName);
        File defaultDir = new File(new File(bigQueryProperties.getDataDir(), fullTableName), BASIC_ISO_DATE.format(LocalDate.now()));
        Map<File, List<String>> result = new HashMap<>();
        try {
            Map<File, List<String>> dirMapping = new HashMap<>();
            for (String csvRow : csvRows) {
                File dir = defaultDir;
                if (createdDateIndex != null) {
                    try {
                        String[] tokens = csvRow.split("[|]");
                        tokens = tokens[createdDateIndex].split(" ");
                        String createdDate = tokens[0].replaceAll("-", "");
                        dir = new File(new File(bigQueryProperties.getDataDir(), fullTableName), createdDate);
                    } catch (RuntimeException ex) {
                        log.warn("Error getting createdDate index for table={}", fullTableName, ex);
                    }
                }
                dirMapping.putIfAbsent(dir, new ArrayList<>());
                dirMapping.get(dir).add(csvRow);
            }
            for (File dir : dirMapping.keySet()) {
                forceMkdir(dir);
                result.put(new File(dir, String.format("%s_%s.csv", fullTableName, shortUUID())), dirMapping.get(dir));
            }
        } catch (RuntimeException | IOException e) {
            log.error("Error occurred trying to create csv file for fullTableName={}", fullTableName, e);
        }
        return result;
    }

    private void writeToBigQuery(String fullTableName, File file) {
        log.debug("Loading file={}", file);
        if (file.exists()) {
            Instant start = Instant.now();
            try {
                String[] tokens = fullTableName.split("\\.");
                TableId tableId = TableId.of(tokens[0], tokens[1]);
                WriteChannelConfiguration writeChannelConfiguration = newBuilder(tableId).setFormatOptions(formatOptions).build();
                JobId jobId = JobId.newBuilder().setJob(BigQueryUtil.shortUUID()).setLocation(bigQueryProperties.getLocation()).build();
                TableDataWriteChannel writer = bigQuery.writer(jobId, writeChannelConfiguration);
                try (OutputStream stream = Channels.newOutputStream(writer)) {
                    Files.copy(file.toPath(), stream);
                }
                Job job = bigQuery.getJob(jobId);
                if (job != null) {
                    Job completedJob = job.waitFor();
                    if (completedJob == null) {
                        log.error("Job no longer exists for file={} table={}", file.getAbsolutePath(), fullTableName);
                        return;
                    } else if (completedJob.getStatus().getError() != null) {
                        log.error("BigQuery was unable to load local file = {} to the table due to an error:{}",
                                file.getAbsolutePath(), job.getStatus().getError());
                        return;
                    }
                    JobStatistics.LoadStatistics stats = completedJob.getStatistics();
                    log.info("BigQuery jobId={} outputRows={} badRecords={} creationTime={} inserted into table={}.{} file={} took {}s",
                            completedJob.getJobId(),
                            stats.getOutputRows(),
                            stats.getBadRecords(),
                            Instant.ofEpochMilli(stats.getCreationTime()),
                            tableId.getDataset(),
                            tableId.getTable(),
                            file.getAbsolutePath(),
                            TimeUnit.MILLISECONDS.toSeconds(Duration.between(start, Instant.now()).toMillis()));
                    try {
                        Files.move(file.toPath(), Path.of(file.getAbsolutePath().replace(".csv", ".copied.csv")));
                    } catch (IOException ex) {
                        log.error("Error occurred while trying to rename file={}", file.getAbsolutePath(), ex);
                    }
                }
            } catch (BigQueryException | IOException | InterruptedException ex) {
                log.error("Error occurred while trying to writeToBigQuery file={} to table={}", file.getAbsolutePath(), fullTableName, ex);
                String message = String.format("Error writing file=%s to bigQuery rootCause=%s", file.getAbsolutePath(), ex);
                emailNotifier.sendMessageAsync(message, "Big Query write error", emailProperties.getTo());
            }
        }
        log.debug("Successfully Loaded file={}", file);
    }

    public void insertRows(String datasetName, String tableName, List<String> csvRows) {
        checkNotNull(datasetName, "datasetName must be provided");
        checkNotNull(tableName, "tableName must be provided");
        if (!CollectionUtils.isEmpty(csvRows)) {
            String fullTableName = String.format("%s.%s", datasetName, tableName);
            countersPerTable.putIfAbsent(fullTableName, new AtomicLong(0));
            if (!dataPerTable.containsKey(fullTableName)) {
                HTreeMap<Long, String> tableRecordMapDb = mapdbForBigQueryRecords.hashMap(fullTableName).keySerializer(Serializer.LONG).valueSerializer(Serializer.STRING).createOrOpen();
                dataPerTable.put(fullTableName, tableRecordMapDb);
            }
            AtomicLong rowCounter = countersPerTable.get(fullTableName);
            csvRows.forEach(row -> dataPerTable.get(fullTableName).put(rowCounter.incrementAndGet(), row));
        }
    }

    public void insertRow(String datasetName, String tableName, String csvRow) {
        checkNotNull(datasetName, "datasetName must be provided");
        checkNotNull(tableName, "tableName must be provided");

        if (StringUtils.trimToNull(csvRow) != null) {
            insertRows(datasetName, tableName, Collections.singletonList(csvRow));
        }
    }

    @PreDestroy
    public void shutDown() {
        loaderScheduler.shutdown();
        filerScheduler.shutdown();
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

    public TableResult performQuery(String query, long pageSize, String tableName) throws InterruptedException {
        TableId tableId = TableId.of(TEMP_DATASET, tableName);
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                .setDestinationTable(tableId)
                .build();
        bigQuery.query(queryConfig);
        return bigQuery.listTableData(tableId, BigQuery.TableDataListOption.pageSize(pageSize));
    }

    public boolean deleteQueryTable(String tableName) {
        return bigQuery.delete(TableId.of(TEMP_DATASET, tableName));
    }

    private class BigQueryFilerTask implements Runnable {
        @Override
        public void run() {
            log.debug("Start BigQueryFilerTask");
            try {
                dataPerTable.forEach((tableName, tableData) -> {
                    List<Long> rowIds = Lists.newArrayList(tableData.keySet());
                    List<String> csvRows = rowIds.stream().map(tableData::get).collect(Collectors.toList());
                    if (!csvRows.isEmpty()) {
                        createFile(tableName, csvRows);
                        rowIds.forEach(tableData::remove);
                    }
                });
            } catch (RuntimeException ex) {
                log.error("Error exporting dataPerTable to bigQuery", ex);
            }
        }
    }

    private class BigQueryLoaderTask implements Runnable {
        private final String fullTableName;
        private final File file;

        private BigQueryLoaderTask(String fullTableName, File file) {
            this.fullTableName = fullTableName;
            this.file = file;
        }

        @Override
        public void run() {
            try {
                log.debug("Start BigQueryLoaderTask");
                writeToBigQuery(fullTableName, file);
            } catch (RuntimeException ex) {
                log.error("Error loading files to bigQuery", ex);
            } finally {
                log.debug("Finish BigQueryLoaderTask");
            }
        }
    }
}
