package com.asbresearch.betfair.esa.cache.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelPriceSize {
    private final int level;
    private final double price;
    private final double size;

    public LevelPriceSize(List<Double> levelPriceSize) {
        level = levelPriceSize.get(0).intValue();
        price = levelPriceSize.get(1);
        size = levelPriceSize.get(2);
    }

    public LevelPriceSize(int level, double price, double size) {
        this.level = level;
        this.price = price;
        this.size = size;
    }

    @Override
    public String toString() {
        return String.format("%d:%f@%f", level, size, price);
    }
}
