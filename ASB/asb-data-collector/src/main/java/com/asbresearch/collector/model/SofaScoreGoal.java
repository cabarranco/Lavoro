package com.asbresearch.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreGoal {
    int homeScore;
    int awayScore;
    boolean isHome;
    int addedTime;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreGoal(@JsonProperty("homeScore") int homeScore,
                         @JsonProperty("awayScore") int awayScore,
                         @JsonProperty("isHome") boolean isHome,
                         @JsonProperty("addedTime") int addedTime) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.isHome = isHome;
        this.addedTime = addedTime;
    }
}
