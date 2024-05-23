package com.asbresearch.collector.mercurius;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@AllArgsConstructor
@Builder(toBuilder = true)
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MercuriusPrediction {
    @EqualsAndHashCode.Include
    private Integer eventId;
    @EqualsAndHashCode.Include
    private String runnerName;
    private Double backFairPrice;

    public String toCsv() {
        return String.format("%s|%s|%s",
                eventId == null ? "" : eventId,
                runnerName == null ? "" : runnerName,
                backFairPrice == null ? "" : backFairPrice);
    }
}
