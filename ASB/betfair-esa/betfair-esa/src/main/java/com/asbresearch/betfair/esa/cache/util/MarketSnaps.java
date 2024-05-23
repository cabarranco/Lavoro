package com.asbresearch.betfair.esa.cache.util;

import com.asbresearch.betfair.esa.cache.market.Market;
import com.asbresearch.betfair.esa.cache.market.MarketCache;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MarketSnaps {
    private final Map<String, MarketCache> markets = new ConcurrentHashMap<>();

    public Optional<MarketSnap> getMarketSnap(String marketId) {
        MarketCache marketCache = markets.get(marketId);
        if (marketCache != null) {
            Market market = marketCache.getMarket(marketId);
            if (market != null) {
                return Optional.ofNullable(market.getSnap());
            }
        }
        return Optional.empty();
    }

    public void put(String marketId, MarketCache marketCache) {
        markets.put(marketId, marketCache);
    }

    public Set<String> getMarketIds() {
        return Collections.unmodifiableSet(markets.keySet());
    }

    public void remove(String marketId) {
        if (marketId != null) {
            markets.remove(marketId);
        }
    }
}
