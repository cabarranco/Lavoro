package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SofaScoreEvent {
    SofaScoreTournament tournament;
    SofaScoreTeam homeTeam;
    SofaScoreTeam awayTeam;
    @EqualsAndHashCode.Include
    long id;
    long startTimestamp;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreEvent(@JsonProperty("tournament") SofaScoreTournament tournament,
                          @JsonProperty("homeTeam") SofaScoreTeam homeTeam,
                          @JsonProperty("awayTeam") SofaScoreTeam awayTeam,
                          @JsonProperty("id") long id,
                          @JsonProperty("startTimestamp") long startTimestamp) {
        this.tournament = tournament;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.id = id;
        this.startTimestamp = startTimestamp;
    }
}
