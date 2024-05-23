package com.asb.analytics.bigquery;

public class AccountBalance {

    private String datetime;
    private String username;
    private Double availableToBet;
    private String currency;
    private Double balanceSaving;
    private Double tradingDayAvailableBalance;

    public AccountBalance(
            String datetime,
            String username,
            Double availableToBet,
            String currency,
            Double balanceSaving,
            Double tradingDayAvailableBalance
    ) {
        this.datetime = datetime;
        this.username = username;
        this.availableToBet = availableToBet;
        this.currency = currency;
        this.balanceSaving = balanceSaving;
        this.tradingDayAvailableBalance = tradingDayAvailableBalance;
    }

    public AccountBalance(
            String datetime,
            String username,
            Double availableToBet,
            String currency
    ) {
        this.datetime = datetime;
        this.username = username;
        this.availableToBet = availableToBet;
        this.currency = currency;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Double getAvailableToBet() {
        return availableToBet;
    }

    public void setAvailableToBet(Double availableToBet) {
        this.availableToBet = availableToBet;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getBalanceSaving() {
        return balanceSaving;
    }

    public void setBalanceSaving(Double balanceSaving) {
        this.balanceSaving = balanceSaving;
    }

    public Double getTradingDayAvailableBalance() {
        return tradingDayAvailableBalance;
    }

    public void setTradingDayAvailableBalance(Double tradingDayAvailableBalance) {
        this.tradingDayAvailableBalance = tradingDayAvailableBalance;
    }
}
