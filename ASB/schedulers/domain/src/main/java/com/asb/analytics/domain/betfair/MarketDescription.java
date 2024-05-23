package com.asb.analytics.domain.betfair;

import java.util.Date;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class MarketDescription {

    private boolean persistenceEnabled;

    private boolean bspMarket;

    private Date marketTime;

    private Date suspendTime;

    private Date settleTime;

    private String bettingType;

    private boolean turnInPlayEnabled;

    private String marketType;

    private String regulator;

    private double marketBaseRate;

    private boolean discountAllowed;

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    public boolean isBspMarket() {
        return bspMarket;
    }

    public void setBspMarket(boolean bspMarket) {
        this.bspMarket = bspMarket;
    }

    public Date getMarketTime() {
        return marketTime;
    }

    public void setMarketTime(Date marketTime) {
        this.marketTime = marketTime;
    }

    public Date getSuspendTime() {
        return suspendTime;
    }

    public void setSuspendTime(Date suspendTime) {
        this.suspendTime = suspendTime;
    }

    public Date getSettleTime() {
        return settleTime;
    }

    public void setSettleTime(Date settleTime) {
        this.settleTime = settleTime;
    }

    public String getBettingType() {
        return bettingType;
    }

    public void setBettingType(String bettingType) {
        this.bettingType = bettingType;
    }

    public boolean isTurnInPlayEnabled() {
        return turnInPlayEnabled;
    }

    public void setTurnInPlayEnabled(boolean turnInPlayEnabled) {
        this.turnInPlayEnabled = turnInPlayEnabled;
    }

    public String getMarketType() {
        return marketType;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getRegulator() {
        return regulator;
    }

    public void setRegulator(String regulator) {
        this.regulator = regulator;
    }

    public double getMarketBaseRate() {
        return marketBaseRate;
    }

    public void setMarketBaseRate(double marketBaseRate) {
        this.marketBaseRate = marketBaseRate;
    }

    public boolean isDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }
}
