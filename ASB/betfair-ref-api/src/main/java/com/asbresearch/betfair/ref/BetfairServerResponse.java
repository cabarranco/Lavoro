package com.asbresearch.betfair.ref;

import java.time.Instant;
import lombok.Value;

@Value
public class BetfairServerResponse<T> {
    private final T response;
    private final Instant lastByte;
    private final Instant requestStart;
    private final long latencyMs;
    private final Boolean hasError;

    public BetfairServerResponse(
            T response,
            Instant lastByte,
            Instant requestStart,
            long latencyMs,
            Boolean hasError) {
        this.response = response;
        this.lastByte = lastByte;
        this.requestStart = requestStart;
        this.latencyMs = latencyMs;
        this.hasError = hasError;
    }
}
