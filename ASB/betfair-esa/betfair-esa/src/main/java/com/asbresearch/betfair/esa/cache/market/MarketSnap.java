package com.asbresearch.betfair.esa.cache.market;

import com.betfair.esa.swagger.model.MarketDefinition;
import java.time.Instant;
import java.util.List;
import lombok.Value;

/**
 * Thread safe atomic snapshot of a market.
 * Reference only changes if the snapshot changes: i.e. if snap1 == snap2 then they are the same (same is true for sub-objects)
 */
@Value
public class MarketSnap {
    private final Instant publishTime;
    private final String marketId;
    private final MarketDefinition marketDefinition;
    private final List<MarketRunnerSnap> marketRunners;
    private final double tradedVolume;
}
