package com.asbresearch.collector.betfair.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@Builder(toBuilder = true)
public class BetfairMarketCatalogueRecord {
    private final BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private Instant startTime;
    private String competition;
    private String eventName;
    private String eventId;
    private String marketName;
    private String marketId;
    private String runnerName;
    private String selectionId;
    private String asbSelectionId;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(startTime),
                csvValue(competition),
                csvValue(eventName),
                csvValue(eventId),
                csvValue(marketName),
                csvValue(marketId),
                csvValue(runnerName),
                csvValue(selectionId),
                csvValue(asbSelectionId),
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }
}
