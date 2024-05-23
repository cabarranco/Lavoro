package com.asbresearch.collector.derived;

import lombok.Value;

import java.time.Instant;

@Value
public class EventPeriod {
    private Instant kickOffTime;
    private Instant secondHalfEndTime;
}
