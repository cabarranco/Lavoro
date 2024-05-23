package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketTypeResult {
    private final String marketType;
    private final int marketCount;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MarketTypeResult(@JsonProperty("marketType") String marketType,
                            @JsonProperty("marketCount") int marketCount) {
        this.marketType = marketType;
        this.marketCount = marketCount;
    }
}
