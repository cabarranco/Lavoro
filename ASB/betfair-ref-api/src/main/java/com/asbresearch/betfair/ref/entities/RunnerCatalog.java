package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunnerCatalog {
    @EqualsAndHashCode.Include
    private final Long selectionId;
    private final String runnerName;
    private final Double handicap;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunnerCatalog(@JsonProperty("selectionId") Long selectionId,
                         @JsonProperty("runnerName") String runnerName,
                         @JsonProperty("handicap") Double handicap) {
        this.selectionId = selectionId;
        this.runnerName = runnerName;
        this.handicap = handicap;
    }

    public static RunnerCatalog of(Long selectionId, String runnerName) {
        return new RunnerCatalog(selectionId, runnerName, 0.0);
    }
}