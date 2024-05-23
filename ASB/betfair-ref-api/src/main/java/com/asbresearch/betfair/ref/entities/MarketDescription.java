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
public class MarketDescription {
    private final Boolean persistenceEnabled;
    private final Boolean bspMarket;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant marketTime;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant suspendTime;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant settleTime;
    private final String bettingType;
    private final Boolean turnInPlayEnabled;
    private final String marketType;
    private final String regulator;
    private final Double marketBaseRate;
    private final Boolean discountAllowed;
    private final String wallet;
    private final String rules;
    private final Boolean rulesHasDate;
    private final String clarifications;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MarketDescription(@JsonProperty("persistenceEnabled") Boolean persistenceEnabled,
                             @JsonProperty("bspMarket") Boolean bspMarket,
                             @JsonProperty("marketTime") Instant marketTime,
                             @JsonProperty("suspendTime") Instant suspendTime,
                             @JsonProperty("settleTime") Instant settleTime,
                             @JsonProperty("bettingType") String bettingType,
                             @JsonProperty("turnInPlayEnabled") Boolean turnInPlayEnabled,
                             @JsonProperty("marketType") String marketType,
                             @JsonProperty("regulator") String regulator,
                             @JsonProperty("marketBaseRate") Double marketBaseRate,
                             @JsonProperty("discountAllowed") Boolean discountAllowed,
                             @JsonProperty("wallet") String wallet,
                             @JsonProperty("rules") String rules,
                             @JsonProperty("rulesHasDate") Boolean rulesHasDate,
                             @JsonProperty("clarifications") String clarifications) {
        this.persistenceEnabled = persistenceEnabled;
        this.bspMarket = bspMarket;
        this.marketTime = marketTime;
        this.suspendTime = suspendTime;
        this.settleTime = settleTime;
        this.bettingType = bettingType;
        this.turnInPlayEnabled = turnInPlayEnabled;
        this.marketType = marketType;
        this.regulator = regulator;
        this.marketBaseRate = marketBaseRate;
        this.discountAllowed = discountAllowed;
        this.wallet = wallet;
        this.rules = rules;
        this.rulesHasDate = rulesHasDate;
        this.clarifications = clarifications;
    }
}
