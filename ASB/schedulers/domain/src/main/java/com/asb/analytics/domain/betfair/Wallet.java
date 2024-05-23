package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wallet {

    private Double availableToBetBalance;

    private Double exposure;

    private Double retainedCommission;

    private Double exposureLimit;

    private Double discountRate;

    private Double pointsBalance;

    private String wallet;

    // GETTER & SETTERS


    public Double getAvailableToBetBalance() {
        return availableToBetBalance;
    }

    public void setAvailableToBetBalance(Double availableToBetBalance) {
        this.availableToBetBalance = availableToBetBalance;
    }

    public Double getExposure() {
        return exposure;
    }

    public void setExposure(Double exposure) {
        this.exposure = exposure;
    }

    public Double getRetainedCommission() {
        return retainedCommission;
    }

    public void setRetainedCommission(Double retainedCommission) {
        this.retainedCommission = retainedCommission;
    }

    public Double getExposureLimit() {
        return exposureLimit;
    }

    public void setExposureLimit(Double exposureLimit) {
        this.exposureLimit = exposureLimit;
    }

    public Double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Double discountRate) {
        this.discountRate = discountRate;
    }

    public Double getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Double pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }
}
