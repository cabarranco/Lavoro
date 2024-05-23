package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties
public class Instruction {

    private long selectionId;

    private String side;

    private String orderType;

    private LimitOrder limitOrder;

    // CONSTRUCTOR

    Instruction(long selectionId, String side, String orderType, LimitOrder limitOrder) {
        this.selectionId = selectionId;
        this.side = side;
        this.orderType = orderType;
        this.limitOrder = limitOrder;
    }

    public Instruction() {
    }

    // GETTERS & SETTERS

    public long getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(long selectionId) {
        this.selectionId = selectionId;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public LimitOrder getLimitOrder() {
        return limitOrder;
    }

    public void setLimitOrder(LimitOrder limitOrder) {
        this.limitOrder = limitOrder;
    }
}
