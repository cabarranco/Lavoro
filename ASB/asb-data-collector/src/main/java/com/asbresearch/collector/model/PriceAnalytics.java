package com.asbresearch.collector.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class PriceAnalytics {
    @EqualsAndHashCode.Include
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private Instant timestamp;
    private String asbSelectionId;
    private Double backPrice;
    private Double layPrice;
    private Double backSize;
    private Double laySize;
    private Double spreadPrice;
    private Double deltaBackPrice;
    private Double deltaLayPrice;
    private Double deltaBackSize;
    private Double deltaLaySize;
    private Double deltaSpreadPrice;
    private Double muBackPrice;
    private Double muLayPrice;
    private Double muBackSize;
    private Double muLaySize;
    private Double muSpreadPrice;
    private Double sigmaBackPrice;
    private Double sigmaLayPrice;
    private Double sigmaBackSize;
    private Double sigmaLaySize;
    private Double sigmaSpreadPrice;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(timestamp),
                csvValue(asbSelectionId),
                csvValue(backPrice),
                csvValue(layPrice),
                csvValue(backSize),
                csvValue(laySize),
                csvValue(spreadPrice),
                csvValue(deltaBackPrice),
                csvValue(deltaLayPrice),
                csvValue(deltaBackSize),
                csvValue(deltaLaySize),
                csvValue(deltaSpreadPrice),
                csvValue(muBackPrice),
                csvValue(muLayPrice),
                csvValue(muBackSize),
                csvValue(muLaySize),
                csvValue(muSpreadPrice),
                csvValue(sigmaBackPrice),
                csvValue(sigmaLayPrice),
                csvValue(sigmaBackSize),
                csvValue(sigmaLaySize),
                csvValue(sigmaSpreadPrice),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }
}
