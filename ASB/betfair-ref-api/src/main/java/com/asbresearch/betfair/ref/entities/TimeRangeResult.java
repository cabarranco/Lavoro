package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeRangeResult {
    private final TimeRange timeRange;
    private final int marketCount;
}
