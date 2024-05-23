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
public class ReplaceInstruction {
    private final String betId;
    private final double newPrice;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReplaceInstruction(@JsonProperty("betId") String betId, @JsonProperty("newPrice") double newPrice) {
        this.betId = betId;
        this.newPrice = newPrice;
    }
}
