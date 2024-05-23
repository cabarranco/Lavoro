package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDescription {
    private final String eventTypeDesc;
    private final String eventDesc;
    private final String marketDesc;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant marketStartTime;
    private final String runnerDesc;
    private int numberOfWinners;

    @JsonCreator(mode = PROPERTIES)
    public ItemDescription(@JsonProperty("eventTypeDesc") String eventTypeDesc,
                           @JsonProperty("eventDesc") String eventDesc,
                           @JsonProperty("marketDesc") String marketDesc,
                           @JsonProperty("marketStartTime") Instant marketStartTime,
                           @JsonProperty("runnerDesc") String runnerDesc,
                           @JsonProperty("numberOfWinners") int numberOfWinners) {
        this.eventTypeDesc = eventTypeDesc;
        this.eventDesc = eventDesc;
        this.marketDesc = marketDesc;
        this.marketStartTime = marketStartTime;
        this.runnerDesc = runnerDesc;
        this.numberOfWinners = numberOfWinners;
    }
}

