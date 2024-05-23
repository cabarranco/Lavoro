package com.asbresearch.pulse.model;

import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.google.common.base.Preconditions.checkNotNull;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OpportunityBet {
    @EqualsAndHashCode.Include
    private final Event event;
    @EqualsAndHashCode.Include
    private final String strategyId;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    @EqualsAndHashCode.Include
    private final Instant timeStamp;
    private final List<OpportunitySelection> selections;
    private final String allocatorId;
    private final String opportunityId;
    private final Boolean inPlay;

    @JsonCreator(mode = PROPERTIES)
    public OpportunityBet(@JsonProperty("event") Event event,
                          @JsonProperty("strategyId") String strategyId,
                          @JsonProperty("timeStamp") Instant timeStamp,
                          @JsonProperty("selections") List<OpportunitySelection> selections,
                          @JsonProperty("allocatorId") String allocatorId,
                          @JsonProperty("opportunityId") String opportunityId,
                          @JsonProperty("inPlay") Boolean inPlay) {
        checkNotNull(event, "event must be provided");
        checkNotNull(strategyId, "strategyId must be provided");
        checkNotNull(timeStamp, "timeStamp must be provided");
        checkNotNull(selections, "selections must be provided");
        checkNotNull(allocatorId, "allocatorId must be provided");
        checkNotNull(inPlay, "inPlay must be provided");

        this.event = event;
        this.strategyId = strategyId;
        this.timeStamp = timeStamp;
        this.selections = selections != null ? ImmutableList.copyOf(selections) : ImmutableList.of();
        this.allocatorId = allocatorId;
        this.opportunityId = opportunityId;
        this.inPlay = inPlay;
    }
}
