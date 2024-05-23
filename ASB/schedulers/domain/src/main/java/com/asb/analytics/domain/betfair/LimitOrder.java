package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties
public class LimitOrder {

    private double size;

    private double price;

    private String persistenceType;

    // CONSTRUCTOR

    LimitOrder(double size, double price, String persistenceType) {
        this.size = size;
        this.price = price;
        this.persistenceType = persistenceType;
    }

    LimitOrder() {

    }

    // GETTERS & SETTERS

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPersistenceType() {
        return persistenceType;
    }

    public void setPersistenceType(String persistenceType) {
        this.persistenceType = persistenceType;
    }
}
