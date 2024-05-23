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

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {
    private final String betId;
    private final String matchId;
    private final String side;
    private final Double price;
    private final Double Size;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant matchDate;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Match(@JsonProperty("betId") String betId,
                 @JsonProperty("matchId") String matchId,
                 @JsonProperty("side") String side,
                 @JsonProperty("price") Double price,
                 @JsonProperty("size") Double size,
                 @JsonProperty("matchDate") Instant matchDate) {
        this.betId = betId;
        this.matchId = matchId;
        this.side = side;
        this.price = price;
        Size = size;
        this.matchDate = matchDate;
    }
}
