package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Competition {
    private final String id;
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Competition(@JsonProperty("id") String id,
                       @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
