package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangePrices {
    private final List<PriceSize> availableToBack;
    private final List<PriceSize> availableToLay;
    private final List<PriceSize> tradedVolume;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ExchangePrices(@JsonProperty("availableToBack") List<PriceSize> availableToBack,
                          @JsonProperty("availableToLay") List<PriceSize> availableToLay,
                          @JsonProperty("tradedVolume") List<PriceSize> tradedVolume) {
        this.availableToBack = availableToBack != null ? ImmutableList.copyOf(availableToBack) : ImmutableList.of();
        this.availableToLay = availableToLay != null ? ImmutableList.copyOf(availableToLay) : ImmutableList.of();
        this.tradedVolume = tradedVolume != null ? ImmutableList.copyOf(tradedVolume) : ImmutableList.of();
    }
}
