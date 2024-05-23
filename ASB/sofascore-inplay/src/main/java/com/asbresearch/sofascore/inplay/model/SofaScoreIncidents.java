package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreIncidents {
    List<SofaScoreIncident> incidents;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreIncidents(@JsonProperty("incidents") List<SofaScoreIncident> incidents) {
        this.incidents = incidents;
    }
}
