package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Runner {
    private final Long selectionId;
    private final Double handicap;
    private final String status;
    private final Double adjustmentFactor;
    private final Double lastPriceTraded;
    private final Double totalMatched;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant removalDate;
    private final StartingPrices sp;
    private final ExchangePrices ex;
    private final List<Order> orders;
    private final List<Match> matches;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Runner(@JsonProperty("selectionId") Long selectionId,
                  @JsonProperty("handicap") Double handicap,
                  @JsonProperty("status") String status,
                  @JsonProperty("adjustmentFactor") Double adjustmentFactor,
                  @JsonProperty("lastPriceTraded") Double lastPriceTraded,
                  @JsonProperty("totalMatched") Double totalMatched,
                  @JsonProperty("removalDate") Instant removalDate,
                  @JsonProperty("sp") StartingPrices sp,
                  @JsonProperty("ex") ExchangePrices ex,
                  @JsonProperty("orders") List<Order> orders,
                  @JsonProperty("matches") List<Match> matches) {
        this.selectionId = selectionId;
        this.handicap = handicap;
        this.status = status;
        this.adjustmentFactor = adjustmentFactor;
        this.lastPriceTraded = lastPriceTraded;
        this.totalMatched = totalMatched;
        this.removalDate = removalDate;
        this.sp = sp;
        this.ex = ex;
        this.orders = orders != null ? ImmutableList.copyOf(orders) : ImmutableList.of();
        this.matches = matches != null ? ImmutableList.copyOf(matches) : ImmutableList.of();
    }
}
