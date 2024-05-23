package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventResponse {

    private Event event;

    private Integer marketCount;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Integer getMarketCount() {
        return marketCount;
    }

    public void setMarketCount(Integer marketCount) {
        this.marketCount = marketCount;
    }

    public boolean isLive() {

        return new Date().getTime() > this.event.getOpenDate().getTime();
    }

    public boolean isOver90() {
        return new Date().getTime() > (this.event.getOpenDate().getTime() + (90 * 60 * 1000) + 1000);
    }
}
