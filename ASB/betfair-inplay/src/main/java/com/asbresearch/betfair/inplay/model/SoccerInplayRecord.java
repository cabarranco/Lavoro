package com.asbresearch.betfair.inplay.model;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.model.BigQueryCreateRecord;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Builder
@Value
public class SoccerInplayRecord {
    public static final String TABLE = BigQueryUtil.SOCCER_INPLAY_TABLE;

    private final BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private final Integer eventId;
    private final Instant updateTime;
    private final Integer matchTime;
    private final String team;
    private final String updateType;
    private final String score;

    public String toString() {
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
