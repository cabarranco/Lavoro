package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSize {
    private final Double price;
    private final Double size;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PriceSize(@JsonProperty("price") Double price, @JsonProperty("size") Double size) {
        this.price = price;
        this.size = size;
    }
}
