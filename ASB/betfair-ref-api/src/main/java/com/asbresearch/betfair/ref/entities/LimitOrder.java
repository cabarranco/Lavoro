package com.asbresearch.betfair.ref.entities;


import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class LimitOrder {
    private final double size;
    private final double price;
    private final PersistenceType persistenceType;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LimitOrder(@JsonProperty("size") double size, @JsonProperty("price") double price, @JsonProperty("persistenceType") PersistenceType persistenceType) {
        this.size = size;
        this.price = price;
        this.persistenceType = persistenceType;
    }
}
