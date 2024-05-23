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
public class MarketBook {
    private final String marketId;
    private final Boolean isMarketDataDelayed;
    private final String status;
    private final int betDelay;
    private final Boolean bspReconciled;
    private final Boolean complete;
    private final Boolean inplay;
    private final int numberOfWinners;
    private final int numberOfRunners;
    private final int numberOfActiveRunners;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant lastMatchTime;
    private final Double totalMatched;
    private final Double totalAvailable;
    private final Boolean crossMatching;
    private final Boolean runnersVoidable;
    private final Long version;
    private final List<Runner> runners;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MarketBook(@JsonProperty("marketId") String marketId,
                      @JsonProperty("isMarketDataDelayed") Boolean isMarketDataDelayed,
                      @JsonProperty("status") String status,
                      @JsonProperty("betDelay") int betDelay,
                      @JsonProperty("bspReconciled") Boolean bspReconciled,
                      @JsonProperty("complete") Boolean complete,
                      @JsonProperty("inplay") Boolean inplay,
                      @JsonProperty("numberOfWinners") int numberOfWinners,
                      @JsonProperty("numberOfRunners") int numberOfRunners,
                      @JsonProperty("numberOfActiveRunners") int numberOfActiveRunners,
                      @JsonProperty("lastMatchTime") Instant lastMatchTime,
                      @JsonProperty("totalMatched") Double totalMatched,
                      @JsonProperty("totalAvailable") Double totalAvailable,
                      @JsonProperty("crossMatching") Boolean crossMatching,
                      @JsonProperty("runnersVoidable") Boolean runnersVoidable,
                      @JsonProperty("version") Long version,
                      @JsonProperty("runners") List<Runner> runners) {
        this.marketId = marketId;
        this.isMarketDataDelayed = isMarketDataDelayed;
        this.status = status;
        this.betDelay = betDelay;
        this.bspReconciled = bspReconciled;
        this.complete = complete;
        this.inplay = inplay;
        this.numberOfWinners = numberOfWinners;
        this.numberOfRunners = numberOfRunners;
        this.numberOfActiveRunners = numberOfActiveRunners;
        this.lastMatchTime = lastMatchTime;
        this.totalMatched = totalMatched;
        this.totalAvailable = totalAvailable;
        this.crossMatching = crossMatching;
        this.runnersVoidable = runnersVoidable;
        this.version = version;
        this.runners = runners != null ? ImmutableList.copyOf(runners) : ImmutableList.of();
    }


}
