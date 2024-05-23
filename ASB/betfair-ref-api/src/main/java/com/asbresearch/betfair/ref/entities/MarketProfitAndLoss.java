package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketProfitAndLoss {
    private final String marketId;
    private final double commissionApplied;
    private final List<RunnerProfitAndLoss> profitAndLosses;
}
