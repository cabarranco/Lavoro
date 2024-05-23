package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class CancelInstruction {
    private final String betId;
    private final double sizeReduction;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CancelInstruction(@JsonProperty("betId") String betId, @JsonProperty("sizeReduction") double sizeReduction) {
        this.betId = betId;
        this.sizeReduction = sizeReduction;
    }
}
