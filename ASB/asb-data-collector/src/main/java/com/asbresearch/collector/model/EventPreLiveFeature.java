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
public class EventPreLiveFeature {
    @EqualsAndHashCode.Include
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private Instant timestamp;
    private Long minsToEnd;
    private Double volumeMO;
    private Double volumeCS;
    private Double volumeOU05;
    private Double volumeOU15;
    private Double volumeOU25;
    private Double volumeOU35;
    private Double volumeAH;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(timestamp),
                csvValue(minsToEnd),
                csvValue(volumeMO),
                csvValue(volumeCS),
                csvValue(volumeOU05),
                csvValue(volumeOU15),
                csvValue(volumeOU25),
                csvValue(volumeOU35),
                csvValue(volumeAH),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }
}
