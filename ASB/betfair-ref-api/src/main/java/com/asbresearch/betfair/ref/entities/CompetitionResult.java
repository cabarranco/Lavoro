package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionResult {
    private final Competition competition;
    private final int marketCount;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CompetitionResult(@JsonProperty("competition") Competition competition,
                             @JsonProperty("marketCount") int marketCount) {
        this.competition = competition;
        this.marketCount = marketCount;
    }
}


