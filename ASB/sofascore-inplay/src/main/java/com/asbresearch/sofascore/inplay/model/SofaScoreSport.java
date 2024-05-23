package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreSport {
    String name;
    long id;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreSport(@JsonProperty("name") String name,
                          @JsonProperty("id") long id) {
        this.name = name;
        this.id = id;
    }
}
