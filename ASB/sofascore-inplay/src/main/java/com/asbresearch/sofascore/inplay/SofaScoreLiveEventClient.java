package com.asbresearch.sofascore.inplay;

import com.asbresearch.sofascore.inplay.model.SofaScoreLiveEvent;
import feign.RequestLine;

public interface SofaScoreLiveEventClient {
    @RequestLine("GET /api/v1/sport/football/events/live")
    SofaScoreLiveEvent getLiveEvent();
}
