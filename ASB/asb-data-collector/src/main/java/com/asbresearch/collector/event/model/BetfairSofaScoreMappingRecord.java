package com.asbresearch.collector.event.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@Builder(toBuilder = true)
public class BetfairSofaScoreMappingRecord {
    private String betfairEventId;
    private String sofascoreEventId;
    private Instant createTimestamp;

    public String toString() {
        return String.format("%s|%s|%s",
                csvValue(betfairEventId),
                csvValue(sofascoreEventId),
                csvValue(createTimestamp));
    }
}
