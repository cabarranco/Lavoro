package com.asbresearch.sofascore.inplay;

import feign.Param;
import feign.RequestLine;
import feign.Response;

public interface SofaScoreIncidentClient {
    @RequestLine("GET /api/v1/event/{id}/incidents")
    Response getIncidents(@Param("id") String id);
}
