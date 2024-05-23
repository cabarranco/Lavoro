package com.asbresearch.betfair.inplay;

import com.asbresearch.betfair.inplay.model.InPlayResponse;
import feign.QueryMap;
import feign.RequestLine;
import java.util.List;
import java.util.Map;

public interface BetfairInPlayClient {
    @RequestLine("GET /inplayservice/v1/eventTimelines")
    List<InPlayResponse> getScore(@QueryMap Map<String, String> parameters);
}
