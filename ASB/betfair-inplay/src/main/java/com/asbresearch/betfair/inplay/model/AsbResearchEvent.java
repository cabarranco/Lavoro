package com.asbresearch.betfair.inplay.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@Builder(toBuilder = true)
public class AsbResearchEvent {
    private String id;
    private Instant startTime;
    private String countryCode;
    private String homeTeam;
    private String awayTeam;
    private String name;
    private Instant createTimestamp;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s",
                csvValue(id),
                csvValue(startTime),
                csvValue(countryCode),
                csvValue(homeTeam),
                csvValue(awayTeam),
                csvValue(name),
                csvValue(createTimestamp));
    }
}
