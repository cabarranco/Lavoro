package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketBook {

    @JsonIgnore
    private Timestamp date = new Timestamp(new Date().getTime());

    private String marketId;

    private boolean isMarketDataDelayed;

    private boolean inplay;

    private double totalMatched;

    private String status;

    private List<Runner> runners;

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public boolean isMarketDataDelayed() {
        return isMarketDataDelayed;
    }

    public void setMarketDataDelayed(boolean marketDataDelayed) {
        isMarketDataDelayed = marketDataDelayed;
    }

    public List<Runner> getRunners() {
        return runners;
    }

    public void setRunners(List<Runner> runners) {
        this.runners = runners;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public boolean isInplay() {
        return inplay;
    }

    public void setInplay(boolean inplay) {
        this.inplay = inplay;
    }

    public double getTotalMatched() {
        return totalMatched;
    }

    public void setTotalMatched(double totalMatched) {
        this.totalMatched = totalMatched;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
