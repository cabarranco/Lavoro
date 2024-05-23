package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Response instruction from betfair placeOrder
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties
public class PlaceInstructionReport {

    private String status;

    private Instruction instruction;

    private String errorCode;

    private String orderStatus;

    private String betId;

    private Date placedDate;

    private Double averagePriceMatched;

    private Double sizeMatched;

    // GETTERS & SETTERS

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public String getBetId() {
        return betId;
    }

    public void setBetId(String betId) {
        this.betId = betId;
    }

    public Date getPlacedDate() {
        return placedDate;
    }

    public void setPlacedDate(Date placedDate) {
        this.placedDate = placedDate;
    }

    public Double getAveragePriceMatched() {
        return averagePriceMatched;
    }

    public void setAveragePriceMatched(Double averagePriceMatched) {
        this.averagePriceMatched = averagePriceMatched;
    }

    public Double getSizeMatched() {
        return sizeMatched;
    }

    public void setSizeMatched(Double sizeMatched) {
        this.sizeMatched = sizeMatched;
    }
}
