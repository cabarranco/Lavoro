package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartingPrices {
    private final Double nearPrice;
    private final Double farPrice;
    private final List<PriceSize> backStakeTaken;
    private final List<PriceSize> layLiabilityTaken;
    private final Double actualSP;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StartingPrices(@JsonProperty("nearPrice") Double nearPrice,
                          @JsonProperty("farPrice") Double farPrice,
                          @JsonProperty("backStakeTaken") List<PriceSize> backStakeTaken,
                          @JsonProperty("layLiabilityTaken") List<PriceSize> layLiabilityTaken,
                          @JsonProperty("actualSP") Double actualSP) {
        this.nearPrice = nearPrice;
        this.farPrice = farPrice;
        this.backStakeTaken = backStakeTaken != null ? ImmutableList.copyOf(backStakeTaken) : ImmutableList.of();
        this.layLiabilityTaken = layLiabilityTaken != null ? ImmutableList.copyOf(layLiabilityTaken) : ImmutableList.of();
        this.actualSP = actualSP;
    }
}
