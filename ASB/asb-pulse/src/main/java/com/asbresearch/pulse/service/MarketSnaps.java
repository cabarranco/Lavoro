package com.asbresearch.pulse.service;

import com.asbresearch.betfair.esa.cache.market.Market;
import com.asbresearch.betfair.esa.cache.market.MarketCache;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;


@Component
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

    public void clear() {
        markets.clear();
    }

    public void put(String marketId, MarketCache marketCache) {
        markets.put(marketId, marketCache);
    }
}
