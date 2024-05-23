package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class LimitOnCloseOrder {
    private final double liability;
    private final double price;

    @JsonCreator(mode = PROPERTIES)
    public LimitOnCloseOrder(@JsonProperty("liability") double liability, @JsonProperty("price") double price) {
        this.liability = liability;
        this.price = price;
    }
}
