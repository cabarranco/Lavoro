package com.asbresearch.betfair.esa.cache.market;

import com.asbresearch.betfair.esa.cache.util.RunnerId;
import com.asbresearch.betfair.esa.cache.util.Utils;
import com.betfair.esa.swagger.model.MarketChange;
import com.betfair.esa.swagger.model.MarketDefinition;
import com.betfair.esa.swagger.model.RunnerChange;
import com.betfair.esa.swagger.model.RunnerDefinition;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *  Thread safe, reference invariant reference to a market.
 *  Repeatedly calling <see cref="Snap"/> will return atomic snapshots of the market.
 */
public class Market {
    private final String marketId;
    private Map<RunnerId, MarketRunner> marketRunners = new ConcurrentHashMap<>();
    private MarketDefinition marketDefinition;
    private double tv;
    //An atomic snapshot of the state of the market.
    private MarketSnap snap;

    public Market(String marketId) {
        this.marketId = marketId;
    }

    void onMarketChange(MarketChange marketChange, Instant publishTime) {
        //initial image means we need to wipe our data
        boolean isImage = Boolean.TRUE.equals(marketChange.getImg());
        //market definition changed
        Optional.ofNullable(marketChange.getMarketDefinition()).ifPresent(this::onMarketDefinitionChange);
        //runners changed
        Optional.ofNullable(marketChange.getRc()).ifPresent(l -> l.forEach(p -> onPriceChange(isImage, p)));

        tv = Utils.selectPrice(isImage, tv, marketChange.getTv());
        snap = new MarketSnap(publishTime, marketId, marketDefinition, marketRunners.entrySet().stream().map(l -> l.getValue().getSnap()).collect(Collectors.toList()), tv);
    }

    private void onPriceChange(boolean isImage, RunnerChange runnerChange) {
        MarketRunner marketRunner = getOrAdd(new RunnerId(runnerChange.getId(), runnerChange.getHc()));
        //update runner
        marketRunner.onPriceChange(isImage, runnerChange);
    }

    private void onMarketDefinitionChange(MarketDefinition marketDefinition) {
        this.marketDefinition = marketDefinition;
        Optional.ofNullable(marketDefinition.getRunners()).ifPresent(rds -> rds.forEach(this::onRunnerDefinitionChange));
    }

    private void onRunnerDefinitionChange(RunnerDefinition runnerDefinition) {
        MarketRunner marketRunner = getOrAdd(new RunnerId(runnerDefinition.getId(), runnerDefinition.getHc()));
        //update runner
        marketRunner.onRunnerDefinitionChange(runnerDefinition);
    }

    private MarketRunner getOrAdd(RunnerId runnerId){
        MarketRunner runner = marketRunners.computeIfAbsent(runnerId, k -> new MarketRunner(this, k));
        return runner;
    }

    public String getMarketId() {
        return marketId;
    }

    public boolean isClosed(){
        //whether the market is closed
        return (marketDefinition != null && marketDefinition.getStatus() == MarketDefinition.StatusEnum.CLOSED);
    }

    public MarketSnap getSnap() {
        return snap;
    }

    @Override
    public String toString() {
        return "Market{" +
                "marketId='" + marketId + '\'' +
                ", marketRunners=" + marketRunners +
                ", marketDefinition=" + marketDefinition +
                '}';
    }
}


