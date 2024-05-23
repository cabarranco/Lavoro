package com.asbresearch.collector.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Builder
@Value
public class SimBetProfitLoss {
    private final BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private final Instant betTimestamp;
    private final Double allocation;
    private final String winBookRunner;
    private final String eventId;
    private final String eventName;
    private final String score;
    private final String opportunityId;
    private final Boolean isFirstBet;
    private final String strategyId;
    private final Double pl;

    public String toCsv() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()),
                csvValue(betTimestamp),
                csvValue(allocation),
                csvValue(winBookRunner),
                csvValue(eventId),
                csvValue(eventName),
                csvValue(score),
                csvValue(opportunityId),
                csvValue(isFirstBet),
                csvValue(strategyId),
                csvValue(pl));
    }
}
