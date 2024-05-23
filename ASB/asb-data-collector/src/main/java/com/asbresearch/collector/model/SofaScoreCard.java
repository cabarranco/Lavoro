package com.asbresearch.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreCard {
    boolean isHome;
    String incidentClass;
    int time;
    int addedTime;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreCard(@JsonProperty("incidentClass") String incidentClass,
                         @JsonProperty("isHome") boolean isHome,
                         @JsonProperty("time") int time,
                         @JsonProperty("addedTime") int addedTime) {
        this.incidentClass = incidentClass;
        this.isHome = isHome;
        this.time = time;
        this.addedTime = addedTime;
    }
}
