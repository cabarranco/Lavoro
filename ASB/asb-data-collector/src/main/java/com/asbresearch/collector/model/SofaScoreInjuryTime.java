package com.asbresearch.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreInjuryTime {
    int length;
    int time;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreInjuryTime(@JsonProperty("length") int length, @JsonProperty("time") int time) {
        this.length = length;
        this.time = time;
    }
}
