package com.asbresearch.collector.betfair.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RunnerPrice {
    @EqualsAndHashCode.Include
    final Double backPrice;
    @EqualsAndHashCode.Include
    final Double backSize;
    @EqualsAndHashCode.Include
    final Double layPrice;
    @EqualsAndHashCode.Include
    final Double laySize;

    public static RunnerPrice of(Double backPrice, Double backSize, Double layPrice, Double laySize) {
        return new RunnerPrice(backPrice, backSize, layPrice, laySize);
    }
}
