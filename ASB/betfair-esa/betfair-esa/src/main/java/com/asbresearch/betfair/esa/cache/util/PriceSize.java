package com.asbresearch.betfair.esa.cache.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PriceSize {
    public static final PriceSize NULL = new PriceSize(null, null);

    private final Double price;
    private final Double size;

    @JsonCreator
    public PriceSize(@JsonProperty("price") Double price, @JsonProperty("size") Double size) {
        this.price = price;
        this.size = size;
    }

    public static PriceSize from(LevelPriceSize levelPriceSize) {
        return new PriceSize(levelPriceSize.getPrice(), levelPriceSize.getSize());
    }

    @Override
    public String toString() {
        return String.format("%f@%f", size, price);
    }
}