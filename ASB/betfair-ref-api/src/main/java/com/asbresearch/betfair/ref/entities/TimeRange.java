package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class TimeRange {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private final Instant from;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private final Instant to;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TimeRange(@JsonProperty("from") Instant from, @JsonProperty("to") Instant to) {
        this.from = from;
        this.to = to;
    }
}
