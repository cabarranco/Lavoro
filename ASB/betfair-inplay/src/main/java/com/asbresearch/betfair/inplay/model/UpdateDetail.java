package com.asbresearch.betfair.inplay.model;


import com.asbresearch.common.json.InstantDeserializer;
import com.asbresearch.common.json.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
public class UpdateDetail {
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant updateTime;
    private final String team;
    private final String teamName;
    private final int matchTime;
    private final int elapsedRegularTime;
    private final int elapsedAddedTime;
    private final String type;
    private final String updateType;

    @JsonCreator(mode = PROPERTIES)
    public UpdateDetail(@JsonProperty("updateTime") Instant updateTime,
                        @JsonProperty("team") String team,
                        @JsonProperty("teamName") String teamName,
                        @JsonProperty("matchTime") int matchTime,
                        @JsonProperty("elapsedRegularTime") int elapsedRegularTime,
                        @JsonProperty("elapsedAddedTime") int elapsedAddedTime,
                        @JsonProperty("type") String type,
                        @JsonProperty("updateType") String updateType) {
        this.updateTime = updateTime;
        this.team = team;
        this.teamName = teamName;
        this.matchTime = matchTime;
        this.elapsedRegularTime = elapsedRegularTime;
        this.elapsedAddedTime = elapsedAddedTime;
        this.type = type;
        this.updateType = updateType;
    }
}
