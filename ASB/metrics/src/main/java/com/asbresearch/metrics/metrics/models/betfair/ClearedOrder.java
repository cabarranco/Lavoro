package com.asbresearch.metrics.metrics.models.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClearedOrder {

    private String eventTypeId;
    private String eventId;
    private String marketId;
    private Integer selectionId;
    private Double handicap;
    private String betId;
    private String placedDate;
    private String persistenceType;
    private String orderType;
    private String side;
    private String betOutcome;
    private Double priceRequested;
    private String settledDate;
    private String lastMatchedDate;
    private Integer betCount;
    private Double priceMatched;
    private Boolean priceReduced;
    private Double sizeSettled;
    private Double profit;

    public String getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(String eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public Integer getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(Integer selectionId) {
        this.selectionId = selectionId;
    }

    public Double getHandicap() {
        return handicap;
    }

    public void setHandicap(Double handicap) {
        this.handicap = handicap;
    }

    public String getBetId() {
        return betId;
    }

    public void setBetId(String betId) {
        this.betId = betId;
    }

    public String getPlacedDate() {
        return placedDate;
    }

    public void setPlacedDate(String placedDate) {
        this.placedDate = placedDate;
    }

    public String getPersistenceType() {
        return persistenceType;
    }

    public void setPersistenceType(String persistenceType) {
        this.persistenceType = persistenceType;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getBetOutcome() {
        return betOutcome;
    }

    public void setBetOutcome(String betOutcome) {
        this.betOutcome = betOutcome;
    }

    public Double getPriceRequested() {
        return priceRequested;
    }

    public void setPriceRequested(Double priceRequested) {
        this.priceRequested = priceRequested;
    }

    public String getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(String settledDate) {
        this.settledDate = settledDate;
    }

    public String getLastMatchedDate() {
        return lastMatchedDate;
    }

    public void setLastMatchedDate(String lastMatchedDate) {
        this.lastMatchedDate = lastMatchedDate;
    }

    public Integer getBetCount() {
        return betCount;
    }

    public void setBetCount(Integer betCount) {
        this.betCount = betCount;
    }

    public Double getPriceMatched() {
        return priceMatched;
    }

    public void setPriceMatched(Double priceMatched) {
        this.priceMatched = priceMatched;
    }

    public Boolean getPriceReduced() {
        return priceReduced;
    }

    public void setPriceReduced(Boolean priceReduced) {
        this.priceReduced = priceReduced;
    }

    public Double getSizeSettled() {
        return sizeSettled;
    }

    public void setSizeSettled(Double sizeSettled) {
        this.sizeSettled = sizeSettled;
    }

    public Double getProfit() {
        return profit;
    }

    public void setProfit(Double profit) {
        this.profit = profit;
    }
}
