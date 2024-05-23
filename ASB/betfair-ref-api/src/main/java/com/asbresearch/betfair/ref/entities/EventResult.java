package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventResult {
    private final Event event;
    private final int marketCount;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EventResult(@JsonProperty("event") Event event,
                       @JsonProperty("marketCount") int marketCount) {
        this.event = event;
        this.marketCount = marketCount;
    }
}
