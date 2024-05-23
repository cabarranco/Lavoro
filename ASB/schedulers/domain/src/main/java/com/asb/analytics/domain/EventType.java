package com.asb.analytics.domain;

/**
 * This class describes the type of the event we want use for betting
 *
 * @author Claudio Paolicelli
 */
public class EventType {

    private int id;

    /**
     * Name of the type
     *
     * Betfair: name
     */
    private String name;

    /**
     * Betfair: marketCount
     */
    private int marketCount;

    //Getter & Setter


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMarketCount() {
        return marketCount;
    }

    public void setMarketCount(int marketCount) {
        this.marketCount = marketCount;
    }
}
