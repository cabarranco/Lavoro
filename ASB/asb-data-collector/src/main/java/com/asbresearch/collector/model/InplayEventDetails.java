package com.asbresearch.collector.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class InplayEventDetails {
    @EqualsAndHashCode.Include
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private Instant kickOffTime;
    private Instant firstHalfEndTime;
    private Instant secondHalfStartTime;
    private Instant secondHalfEndTime;
    private String scoreFirstHalfEnd;
    private String scoreSecondHalfEnd;
    private String source;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(kickOffTime),
                csvValue(firstHalfEndTime),
                csvValue(secondHalfStartTime),
                csvValue(secondHalfEndTime),
                csvValue(scoreFirstHalfEnd),
                csvValue(scoreSecondHalfEnd),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()),
                csvValue(source));
    }
}
