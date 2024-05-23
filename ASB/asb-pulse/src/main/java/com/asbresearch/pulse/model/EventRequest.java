package com.asbresearch.pulse.model;

import com.asbresearch.betfair.ref.entities.TimeRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Value;

import static com.asbresearch.pulse.util.Constants.DEFAULT_TRADING_TIME_RANGE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class EventRequest {
    private final Set<String> includeCompetitions;
    private final Set<String> excludeCompetitions;
    private final TimeRange timeRange;
    private final Set<String> asbMarketCodes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EventRequest(@JsonProperty("timeRange") TimeRange timeRange,
                        @JsonProperty("includeCompetitions") Set<String> includeCompetitions,
                        @JsonProperty("excludeCompetitions") Set<String> excludeCompetitions,
                        @JsonProperty("asbMarketCodes") Set<String> asbMarketCodes) {
        this.includeCompetitions = includeCompetitions == null ? ImmutableSet.of() : ImmutableSet.copyOf(includeCompetitions);
        this.excludeCompetitions = excludeCompetitions == null ? ImmutableSet.of() : ImmutableSet.copyOf(excludeCompetitions);
        this.timeRange = timeRange == null ? DEFAULT_TRADING_TIME_RANGE : timeRange;
        this.asbMarketCodes = asbMarketCodes == null ? ImmutableSet.of() : ImmutableSet.copyOf(asbMarketCodes);
    }
}
