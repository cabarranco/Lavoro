package com.asbresearch.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScorePeriod {
    String text;
    int time;
    int homeScore;
    int awayScore;

    @JsonCreator(mode = PROPERTIES)
    public SofaScorePeriod(@JsonProperty("text") String text,
                           @JsonProperty("time") int time,
                           @JsonProperty("homeScore") int homeScore,
                           @JsonProperty("awayScore") int awayScore) {
        this.text = text;
        this.time = time;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
