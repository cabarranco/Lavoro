package com.asbresearch.collector.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Builder
@Value
public class SofaScoreSoccerInPlayRecord {
    public static final String TABLE = "sofascore_soccer_inplay";

    private final BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private final String eventId;
    private final Instant updateTime;
    private final Integer matchTime;
    private final String team;
    private final String updateType;
    private final String score;

    public String toCsv() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(updateTime),
                csvValue(matchTime),
                csvValue(team),
                csvValue(updateType),
                csvValue(score),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }
}
