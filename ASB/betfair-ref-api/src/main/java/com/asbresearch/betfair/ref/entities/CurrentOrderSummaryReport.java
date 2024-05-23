package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentOrderSummaryReport {
    private final List<CurrentOrderSummary> currentOrders;
    private final boolean moreAvailable;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CurrentOrderSummaryReport(@JsonProperty("currentOrders") List<CurrentOrderSummary> currentOrders,
                                     @JsonProperty("moreAvailable") boolean moreAvailable) {
        this.currentOrders = currentOrders;
        this.moreAvailable = moreAvailable;
    }
}
