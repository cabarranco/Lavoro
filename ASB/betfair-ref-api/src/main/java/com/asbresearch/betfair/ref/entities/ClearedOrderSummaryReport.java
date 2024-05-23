package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClearedOrderSummaryReport {
    private final boolean moreAvailable;
    private final List<ClearedOrderSummary> clearedOrders;

    @JsonCreator(mode = PROPERTIES)
    public ClearedOrderSummaryReport(@JsonProperty("moreAvailable") boolean moreAvailable,
                                     @JsonProperty("clearedOrders") List<ClearedOrderSummary> clearedOrders) {
        this.moreAvailable = moreAvailable;
        this.clearedOrders = clearedOrders != null ? ImmutableList.copyOf(clearedOrders) : ImmutableList.of();
    }
}

