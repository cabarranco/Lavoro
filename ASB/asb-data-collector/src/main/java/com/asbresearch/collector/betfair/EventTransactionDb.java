package com.asbresearch.collector.betfair;

import com.asbresearch.collector.betfair.model.BetfairHistoricalRecord;
import com.asbresearch.collector.betfair.model.RunnerPrice;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.util.CollectionUtils;
import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.betfair.esa.swagger.model.MarketDefinition;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.asbresearch.collector.derived.AnalyticEventsProvider.eventSql;
import static com.asbresearch.collector.util.Constants.endDate;
import static com.asbresearch.collector.util.Constants.startDate;

@Service("EventTransactionDb")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "eventTransactionDb", havingValue = "on")
@EnableConfigurationProperties({CollectorProperties.class, BigQueryProperties.class})
public class EventTransactionDb {
    private static final String SQL = "select " +
            "eventId, " +
            "marketId, " +
            "asbSelectionId, " +
            "selectionId, " +
            "status, " +
            "inplay, " +
            "totalMatched, " +
            "backPrice, " +
            "backSize, " +
            "layPrice, " +
            "laySize, " +
            "unix_millis(publishTime) as publishTime, " +
            "from `betstore.betfair_historical_data` " +
            "where (date(publishTime) between '%s' and '%s') " +
            "and eventId in (%s) " +
            "order by eventId, asbSelectionId, publishTime desc";

    private static final String EVENT_TRANSACTION = "event_transaction";

    private final BigQueryService bigQueryService;
    private final AtomicBoolean available = new AtomicBoolean(false);
    private final CollectorProperties collectorProperties;
    private final String startDate;
    private final String endDate;
    private final BigQueryProperties bigQueryProperties;
    private DB eventTransactionDb;

    public EventTransactionDb(BigQueryService bigQueryService,
                              CollectorProperties collectorProperties,
                              BigQueryProperties bigQueryProperties) {
        this.bigQueryService = bigQueryService;
        this.collectorProperties = collectorProperties;

        startDate = startDate(collectorProperties);
        endDate = endDate(collectorProperties);
        this.bigQueryProperties = bigQueryProperties;
        eventTransactionDb = createEventTransactionDb();
    }

    private DB createEventTransactionDb() {
        File dbFile = new File(bigQueryProperties.getDataDir(), "mapdb");
        if (!dbFile.exists()) {
            dbFile.mkdirs();
        }
        return DBMaker.fileDB(new File(dbFile, String.format("%s.db", EVENT_TRANSACTION)))
                .fileMmapEnableIfSupported()
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    @SneakyThrows
    public void loadTransactionData() {
        try {
            if (!isDbAvailable()) {
                AtomicLong dbRows = new AtomicLong(0);
                AtomicLong mapDbRows = new AtomicLong(0);
                String sql = String.format(SQL, startDate, endDate, eventSql(collectorProperties));
                log.info("sql={}", sql);
                String queryTableName = BigQueryUtil.shortUUID();
                try {
                    TableResult tableResult = bigQueryService.performQuery(sql, collectorProperties.getHistoricalDataPageSize(), queryTableName);
                    dbRows.addAndGet(tableResult.getTotalRows());
                    String currentEventId = null;
                    Set<String> currentEventTransactions = null;
                    for (FieldValueList row : tableResult.iterateAll()) {
                        FieldValue eventIdValue = row.get(0);
                        if (eventIdValue != null) {
                            if (currentEventId == null || !currentEventId.equals(eventIdValue.getStringValue())) {
                                currentEventId = eventIdValue.getStringValue();
                                currentEventTransactions = eventTransactionDb.treeSet(currentEventId, Serializer.STRING).createOrOpen();
                            }
                        }
                        RunnerPrice runnerPrice = RunnerPrice.of(
                                row.get(7).getDoubleValue(),
                                row.get(8).getDoubleValue(),
                                row.get(9).getDoubleValue(),
                                row.get(10).getDoubleValue());

                        BetfairHistoricalRecord record = BetfairHistoricalRecord.builder()
                                .eventId(row.get(0).getStringValue())
                                .marketId(row.get(1).getStringValue())
                                .asbSelectionId(row.get(2).getStringValue())
                                .selectionId(row.get(3).getLongValue())
                                .status(MarketDefinition.StatusEnum.valueOf(row.get(4).getStringValue()))
                                .inplay(row.get(5).getBooleanValue())
                                .totalMatched(row.get(6).getDoubleValue())
                                .runnerPrice(runnerPrice)
                                .publishTime(Instant.ofEpochMilli(row.get(11).getLongValue()))
                                .build();
                        currentEventTransactions.add(record.toString());
                        mapDbRows.incrementAndGet();
                    }
                    bigQueryService.deleteQueryTable(queryTableName);
                } catch (InterruptedException e) {
                    log.error("Error loading historical data", e);
                }
                log.info("TotalRows From BQ={}, TotalRows Written To MapDB={}", dbRows.get(), mapDbRows.get());
                eventTransactionDb.commit();
                available.set(true);
            }
        } catch (BigQueryException e) {
            log.error("Error loading historical data", e);
        }
    }

    public boolean isDbAvailable() {
        return available.get();
    }

    public List<BetfairHistoricalRecord> getTransactions(String eventId) {
        List<BetfairHistoricalRecord> result = Collections.emptyList();
        if (available.get()) {
            Set<String> eventTrans = eventTransactionDb.treeSet(eventId, Serializer.STRING).createOrOpen();
            if (!CollectionUtils.isEmpty(eventTrans)) {
                result = eventTrans.stream().map(text -> BetfairHistoricalRecord.of(text)).collect(Collectors.toList());
            }
        }
        return result;
    }
}
