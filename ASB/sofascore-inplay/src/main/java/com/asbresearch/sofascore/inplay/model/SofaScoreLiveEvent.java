package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreLiveEvent {
    List<SofaScoreEvent> events;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreLiveEvent(@JsonProperty("events") List<SofaScoreEvent> events) {
        this.events = events;
    }
}
