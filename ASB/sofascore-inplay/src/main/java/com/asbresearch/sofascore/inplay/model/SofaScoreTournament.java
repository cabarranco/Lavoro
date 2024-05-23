package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreTournament {
    String name;
    SofaScoreCategory category;
    long id;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreTournament(@JsonProperty("name") String name,
                             @JsonProperty("category") SofaScoreCategory category,
                             @JsonProperty("id") long id,
                             @JsonProperty("startTimestamp") long startTimestamp) {
        this.name = name;
        this.category = category;
        this.id = id;
    }
}
