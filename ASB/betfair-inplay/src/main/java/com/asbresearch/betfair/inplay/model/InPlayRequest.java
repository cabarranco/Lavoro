package com.asbresearch.betfair.inplay.model;

import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.google.common.base.Preconditions.checkNotNull;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InPlayRequest {
    @EqualsAndHashCode.Include
    private final Integer eventId;
    private final Instant startTime;

    public static InPlayRequest of(Integer eventId) {
        return of(eventId, Instant.now());
    }

    public static InPlayRequest of(Integer eventId, Instant startTime) {
        checkNotNull(eventId, "EventId cannot be blank");
        checkNotNull(startTime, "Start time must be provided");
        return new InPlayRequest(eventId, startTime);
    }
}
