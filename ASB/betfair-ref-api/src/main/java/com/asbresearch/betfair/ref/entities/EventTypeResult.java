package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTypeResult {
    private final EventType eventType;
    private final int marketCount;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EventTypeResult(@JsonProperty("eventType") EventType eventType,
                           @JsonProperty("marketCount") int marketCount) {
        this.eventType = eventType;
        this.marketCount = marketCount;
    }
}
