package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.OrderStatus;
import com.asbresearch.betfair.ref.enums.OrderType;
import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.asbresearch.betfair.ref.enums.Side;
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
public class CurrentOrderSummary {
    private final String BetId;
    private final String marketId;
    private final Long selectionId;
    private final Double handicap;
    private final PriceSize priceSize;
    private final Double bspLiability;
    private final Side side;
    private final OrderStatus status;
    private final PersistenceType persistenceType;
    private final OrderType orderType;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant placedDate;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant matchedDate;
    private final Double averagePriceMatched;
    private final Double sizeMatched;
    private final Double sizeRemaining;
    private final Double sizeLapsed;
    private final Double sizeCancelled;
    private final Double sizeVoided;
    private final String regulatorAuthCode;
    private final String regulatorCode;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CurrentOrderSummary(@JsonProperty("betId") String betId,
                               @JsonProperty("marketId") String marketId,
                               @JsonProperty("selectionId") Long selectionId,
                               @JsonProperty("handicap") Double handicap,
                               @JsonProperty("priceSize") PriceSize priceSize,
                               @JsonProperty("bspLiability") Double bspLiability,
                               @JsonProperty("side") Side side,
                               @JsonProperty("status") OrderStatus status,
                               @JsonProperty("persistenceType") PersistenceType persistenceType,
                               @JsonProperty("orderType") OrderType orderType,
                               @JsonProperty("placedDate") Instant placedDate,
                               @JsonProperty("matchedDate") Instant matchedDate,
                               @JsonProperty("averagePriceMatched") Double averagePriceMatched,
                               @JsonProperty("sizeMatched") Double sizeMatched,
                               @JsonProperty("sizeRemaining") Double sizeRemaining,
                               @JsonProperty("sizeLapsed") Double sizeLapsed,
                               @JsonProperty("sizeCancelled") Double sizeCancelled,
                               @JsonProperty("sizeVoided") Double sizeVoided,
                               @JsonProperty("regulatorAuthCode") String regulatorAuthCode,
                               @JsonProperty("regulatorCode") String regulatorCode) {
        BetId = betId;
        this.marketId = marketId;
        this.selectionId = selectionId;
        this.handicap = handicap;
        this.priceSize = priceSize;
        this.bspLiability = bspLiability;
        this.side = side;
        this.status = status;
        this.persistenceType = persistenceType;
        this.orderType = orderType;
        this.placedDate = placedDate;
        this.matchedDate = matchedDate;
        this.averagePriceMatched = averagePriceMatched;
        this.sizeMatched = sizeMatched;
        this.sizeRemaining = sizeRemaining;
        this.sizeLapsed = sizeLapsed;
        this.sizeCancelled = sizeCancelled;
        this.sizeVoided = sizeVoided;
        this.regulatorAuthCode = regulatorAuthCode;
        this.regulatorCode = regulatorCode;
    }
}
