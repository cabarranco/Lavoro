package com.asbresearch.betfair.inplay.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class KickOffTimes {
    private Instant kickOff;
    private Instant secondHalfKickOff;
}
