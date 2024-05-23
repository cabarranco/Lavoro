package com.asbresearch.betfair.esa.cache.market;

import com.asbresearch.betfair.esa.cache.util.RunnerId;
import com.betfair.esa.swagger.model.RunnerDefinition;
import lombok.Value;

/**
 * Thread safe atomic snapshot of a market runner.
   Reference only changes if the snapshot changes: i.e. if snap1 == snap2 then they are the same (same is true for sub-objects)
 */
@Value
public class MarketRunnerSnap {
    private final RunnerId runnerId;
    private final RunnerDefinition definition;
    private final MarketRunnerPrices prices;

    public MarketRunnerSnap(RunnerId runnerId, RunnerDefinition definition, MarketRunnerPrices prices) {
        this.runnerId = runnerId;
        this.definition = definition;
        this.prices = prices;
    }
}
