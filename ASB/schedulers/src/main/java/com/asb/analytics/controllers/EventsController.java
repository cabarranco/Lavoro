package com.asb.analytics.controllers;

import com.asb.analytics.api.betfair.betting.BetfairBetting;
import com.asb.analytics.api.betfair.filters.TimeRange;
import com.asb.analytics.domain.betfair.EventResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class EventsController {

    private String token;
    private String eventId = null;
    private TimeRange timeRange = null;

    public EventsController(String token) {
        this.token = token;
    }

    public EventsController(String token, TimeRange timeRange) {
        this.timeRange = timeRange;
        this.token = token;
    }

    public EventsController(String token, String eventId) {
        this.token = token;
        this.eventId = eventId;
    }

    public List<EventResponse> getSoccerEvents() throws Exception {

        HashMap<String, Object> filtersMapEvents = new HashMap<>();

        filtersMapEvents.put("eventTypeIds", Collections.singletonList("1"));

        if (eventId != null) {
            filtersMapEvents.put("eventIds", Collections.singletonList(eventId));
        }

        if (timeRange != null) {
            filtersMapEvents.put("marketStartTime", timeRange);
        }

        // GET EVENTS

        return BetfairBetting.init(token)
                .getListEvents(filtersMapEvents);

    }
}
