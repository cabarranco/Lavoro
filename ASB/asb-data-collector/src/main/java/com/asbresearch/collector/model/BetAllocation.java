package com.asbresearch.collector.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Builder
@Value
public class BetAllocation {
    private final Instant betTimestamp;
    private final String eventId;
    private final String eventName;
    private final String opportunityId;
    private final Boolean isFirstBet;
    private final String strategyId;
    private final String bookRunner;
    private final Double orderAllocation;
    private final Double orderPrice;
}
