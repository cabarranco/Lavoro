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
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class StatementLegacyData {
    private final double averagePrice;
    private final double betSize;
    private final String betType;
    private final String betCategoryType;
    private final String commissionRate;
    private final long eventId;
    private final long eventTypeId;
    private final String fullMarketName;
    private final double grossBetAmount;
    private final String marketName;
    private final String marketType;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant placedDate;
    private final long selectionId;
    private final String selectionName;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant startDate;
    private final String transactionType;
    @EqualsAndHashCode.Include
    private final long transactionId;
    private final String winLose;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StatementLegacyData(@JsonProperty("averagePrice") double averagePrice,
                               @JsonProperty("betSize") double betSize,
                               @JsonProperty("betType") String betType,
                               @JsonProperty("betCategoryType") String betCategoryType,
                               @JsonProperty("commissionRate") String commissionRate,
                               @JsonProperty("eventId") long eventId,
                               @JsonProperty("eventTypeId") long eventTypeId,
                               @JsonProperty("fullMarketName") String fullMarketName,
                               @JsonProperty("grossBetAmount") double grossBetAmount,
                               @JsonProperty("marketName") String marketName,
                               @JsonProperty("marketType") String marketType,
                               @JsonProperty("placedDate") Instant placedDate,
                               @JsonProperty("selectionId") long selectionId,
                               @JsonProperty("selectionName") String selectionName,
                               @JsonProperty("startDate") Instant startDate,
                               @JsonProperty("transactionType") String transactionType,
                               @JsonProperty("transactionId") long transactionId,
                               @JsonProperty("winLose") String winLose) {
        this.averagePrice = averagePrice;
        this.betSize = betSize;
        this.betType = betType;
        this.betCategoryType = betCategoryType;
        this.commissionRate = commissionRate;
        this.eventId = eventId;
        this.eventTypeId = eventTypeId;
        this.fullMarketName = fullMarketName;
        this.grossBetAmount = grossBetAmount;
        this.marketName = marketName;
        this.marketType = marketType;
        this.placedDate = placedDate;
        this.selectionId = selectionId;
        this.selectionName = selectionName;
        this.startDate = startDate;
        this.transactionType = transactionType;
        this.transactionId = transactionId;
        this.winLose = winLose;
    }
}
