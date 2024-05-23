package com.asbresearch.pulse.service;

import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.RunnerCatalog;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MarketSelection {
    @JsonIgnore
    private final Event event;
    @EqualsAndHashCode.Include
    private final String marketId;
    @EqualsAndHashCode.Include
    private final RunnerCatalog runnerCatalog;
    @JsonIgnore
    private final String marketType;
    @JsonIgnore
    private final UserRunnerCode userRunnerCode;

    public static MarketSelection of(Event event,
                                     String marketId,
                                     RunnerCatalog runnerCatalog,
                                     String marketType,
                                     UserRunnerCode userRunnerCode) {
        return new MarketSelection(event, marketId, runnerCatalog, marketType, userRunnerCode);
    }
}
