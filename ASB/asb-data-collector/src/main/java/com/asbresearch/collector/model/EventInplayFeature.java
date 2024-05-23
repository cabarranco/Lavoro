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
public class EventInplayFeature {
    @EqualsAndHashCode.Include
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private Instant timestamp;
    private Long secondsInPlay;
    private Long minsToEnd;
    private Integer cumYCardsH;
    private Integer cumRCardsH;
    private Integer cumYCardsA;
    private Integer cumRCardsA;
    private Integer cumGoalsH;
    private Integer cumGoalsA;
    private String score;
    private String previousScore;
    private Double volumeMO;
    private Double volumeCS;
    private Double volumeOU05;
    private Double volumeOU15;
    private Double volumeOU25;
    private Double volumeOU35;
    private Double volumeAH;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(timestamp),
                csvValue(secondsInPlay),
                csvValue(minsToEnd),
                csvValue(cumYCardsH),
                csvValue(cumRCardsH),
                csvValue(cumYCardsA),
                csvValue(cumRCardsA),
                csvValue(cumGoalsH),
                csvValue(cumGoalsA),
                csvValue(score),
                csvValue(previousScore),
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
