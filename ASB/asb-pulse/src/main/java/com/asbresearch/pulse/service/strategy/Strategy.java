package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.BetfairEventService;
import com.asbresearch.pulse.service.MarketSnaps;
import com.asbresearch.pulse.service.OpportunityQueue;
import java.util.List;
import java.util.Set;

public interface Strategy {
    void init(BetfairEventService betfairEventService);

    void marketSnapsAndOpportunityQueue(MarketSnaps marketSnaps, OpportunityQueue opportunityQueue);

    Set<String> marketSubscriptions();

    void onMarketChange(MarketSnap marketSnap);

    List<Event> events();

    void shutDown();

    String getId();

    StrategySpec getStrategySpec();
}
