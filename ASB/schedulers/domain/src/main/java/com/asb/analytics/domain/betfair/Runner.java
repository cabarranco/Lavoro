package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Runner {

    private long selectionId;

    private String runnerName;

    private double handicap;

    private int sortPriority;

    private String status;

    private ExchangePrices ex;

    public long getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(long selectionId) {
        this.selectionId = selectionId;
    }

    public double getHandicap() {
        return handicap;
    }

    public void setHandicap(double handicap) {
        this.handicap = handicap;
    }

    public ExchangePrices getEx() {
        return ex;
    }

    public void setEx(ExchangePrices ex) {
        this.ex = ex;
    }

    public double getPrice() {
        return (!this.ex.getAvailableToBack().isEmpty()) ? this.ex.getAvailableToBack().get(0).getPrice() : 0;
    }

    public String getRunnerName() {
        return runnerName;
    }

    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(int sortPriority) {
        this.sortPriority = sortPriority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
