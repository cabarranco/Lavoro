package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketCatalogue {
    private final String marketId;
    private final String marketName;
    private final MarketDescription description;
    private final List<RunnerCatalog> runners;
    private final EventType eventType;
    private final Competition competition;
    private final Event event;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MarketCatalogue(@JsonProperty("marketId") String marketId,
                           @JsonProperty("marketName") String marketName,
                           @JsonProperty("description") MarketDescription description,
                           @JsonProperty("runners") List<RunnerCatalog> runners,
                           @JsonProperty("eventType") EventType eventType,
                           @JsonProperty("competition") Competition competition,
                           @JsonProperty("event") Event event) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.description = description;
        this.runners = runners != null ? ImmutableList.copyOf(runners) : ImmutableList.of();
        this.eventType = eventType;
        this.competition = competition;
        this.event = event;
    }
}
