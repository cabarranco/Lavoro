package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSize {

    private double price;

    private double size;

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }
}
