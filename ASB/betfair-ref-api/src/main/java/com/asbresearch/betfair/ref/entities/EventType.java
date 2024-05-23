package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventType {
    private final String id;
    private final String name;

    @JsonCreator(mode = Mode.PROPERTIES)
    public EventType(@JsonProperty("id") String id,
                     @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
