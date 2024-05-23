package com.asb.analytics.domain.betfair;

import com.asb.analytics.domain.SimpleItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketCatalogue {

    private String marketId;

    private String marketName;

    private Double totalMatched;

    private List<Runner> runners;

    private SimpleItem competition;

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public Double getTotalMatched() {
        return totalMatched;
    }

    public void setTotalMatched(Double totalMatched) {
        this.totalMatched = totalMatched;
    }

    public List<Runner> getRunners() {
        return runners;
    }

    public void setRunners(List<Runner> runners) {
        this.runners = runners;
    }

    public SimpleItem getCompetition() {
        return competition != null ? competition : new SimpleItem("", "");
    }

    public void setCompetition(SimpleItem competition) {
        this.competition = competition;
    }
}
