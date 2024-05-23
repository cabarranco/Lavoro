package com.asbresearch.collector.betfair.model;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.model.BigQueryCreateRecord;
import com.betfair.esa.swagger.model.MarketDefinition.StatusEnum;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.regex.Pattern;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@Builder(toBuilder = true)
public class BetfairHistoricalRecord {
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private String marketId;
    private String asbSelectionId;
    private Long selectionId;
    private StatusEnum status;
    private Boolean inplay;
    private Double totalMatched;
    private RunnerPrice runnerPrice;
    private Instant publishTime;

    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(eventId),
                csvValue(marketId),
                csvValue(asbSelectionId),
                csvValue(selectionId),
                csvValue(status),
                csvValue(inplay),
                csvValue(totalMatched),
                runnerPrice != null ? csvValue(runnerPrice.getBackPrice()) : "",
                runnerPrice != null ? csvValue(runnerPrice.getBackSize()) : "",
                runnerPrice != null ? csvValue(runnerPrice.getLayPrice()) : "",
                runnerPrice != null ? csvValue(runnerPrice.getLaySize()) : "",
                csvValue(publishTime),
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }

    public static BetfairHistoricalRecord of(String text) {
        if (text != null) {
            String[] tokens = text.split(Pattern.quote("|"));
            if (tokens.length == 14) {
                return BetfairHistoricalRecord.builder()
                        .eventId(tokens[0])
                        .marketId(tokens[1])
                        .asbSelectionId(tokens[2])
                        .selectionId(Long.valueOf(tokens[3]))
                        .status(StatusEnum.valueOf(tokens[4]))
                        .inplay(Boolean.valueOf(tokens[5]))
                        .totalMatched(Double.valueOf(tokens[6]))
                        .runnerPrice(RunnerPrice.of(
                                Double.valueOf(tokens[7]),
                                Double.valueOf(tokens[8]),
                                Double.valueOf(tokens[9]),
                                Double.valueOf(tokens[10])))
                        .publishTime(BigQueryUtil.INSTANT_FORMATTER.parse(tokens[11], Instant::from))
                        .build();
            }
        }
        throw new IllegalArgumentException(String.format("text=%s cannot be converted to BetfairHistoricalRecord", text));
    }
}
