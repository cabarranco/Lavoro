package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreCategory {
    String name;
    SofaScoreSport sport;
    long id;
    String flag;
    String alpha2;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreCategory(@JsonProperty("name") String name,
                             @JsonProperty("sport") SofaScoreSport sport,
                             @JsonProperty("id") long id,
                             @JsonProperty("flag") String flag,
                             @JsonProperty("alpha2") String alpha2) {
        this.name = name;
        this.sport = sport;
        this.id = id;
        this.flag = flag;
        this.alpha2 = alpha2;
    }
}
