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
public class Order {
    private final String betId;
    private final String orderType;
    private final String status;
    private final String persistenceType;
    private final String side;
    private final Double price;
    private final Double size;
    private final Double bspLiability;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant placedDate;
    private final Double avgPriceMatched;
    private final Double sizeMatched;
    private final Double sizeRemaining;
    private final Double sizeLapsed;
    private final Double sizeCancelled;
    private final Double sizeVoided;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Order(@JsonProperty("betId") String betId,
                 @JsonProperty("orderType") String orderType,
                 @JsonProperty("status") String status,
                 @JsonProperty("persistenceType") String persistenceType,
                 @JsonProperty("side") String side,
                 @JsonProperty("price") Double price,
                 @JsonProperty("size") Double size,
                 @JsonProperty("bspLiability") Double bspLiability,
                 @JsonProperty("placedDate") Instant placedDate,
                 @JsonProperty("avgPriceMatched") Double avgPriceMatched,
                 @JsonProperty("sizeMatched") Double sizeMatched,
                 @JsonProperty("sizeRemaining") Double sizeRemaining,
                 @JsonProperty("sizeLapsed") Double sizeLapsed,
                 @JsonProperty("sizeCancelled") Double sizeCancelled,
                 @JsonProperty("sizeVoided") Double sizeVoided) {
        this.betId = betId;
        this.orderType = orderType;
        this.status = status;
        this.persistenceType = persistenceType;
        this.side = side;
        this.price = price;
        this.size = size;
        this.bspLiability = bspLiability;
        this.placedDate = placedDate;
        this.avgPriceMatched = avgPriceMatched;
        this.sizeMatched = sizeMatched;
        this.sizeRemaining = sizeRemaining;
        this.sizeLapsed = sizeLapsed;
        this.sizeCancelled = sizeCancelled;
        this.sizeVoided = sizeVoided;
    }
}
