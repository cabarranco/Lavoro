package com.asbresearch.betfair.esa.cache.order;

import com.asbresearch.betfair.esa.cache.util.OrderMarketRunner;
import com.asbresearch.betfair.esa.cache.util.OrderMarketRunnerSnap;
import com.asbresearch.betfair.esa.cache.util.OrderMarketSnap;
import com.asbresearch.betfair.esa.cache.util.RunnerId;
import com.betfair.esa.swagger.model.OrderMarketChange;
import com.betfair.esa.swagger.model.OrderRunnerChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderMarket {

    private final OrderCache orderCache;
    private final String marketId;
    private final Map<RunnerId, OrderMarketRunner> marketRunners = new ConcurrentHashMap<>();
    private OrderMarketSnap orderMarketSnap;
    private boolean isClosed;

    public OrderMarket(OrderCache orderCache, String marketId) {
        this.orderCache = orderCache;
        this.marketId = marketId;
    }

    public void onOrderMarketChange(OrderMarketChange orderMarketChange) {
        OrderMarketSnap newSnap = new OrderMarketSnap();
        newSnap.setMarketId(this.marketId);

        // update runners
        if (orderMarketChange.getOrc() != null) {
            for (OrderRunnerChange orderRunnerChange : orderMarketChange.getOrc()) {
                onOrderRunnerChange(orderRunnerChange);
            }
        }

        List<OrderMarketRunnerSnap> snaps = new ArrayList<>(marketRunners.size());
        for (OrderMarketRunner orderMarketRunner : marketRunners.values()) {
            snaps.add(orderMarketRunner.getOrderMarketRunnerSnap());
        }

        newSnap.setOrderMarketRunners(snaps);

        isClosed = Boolean.TRUE.equals(orderMarketChange.getClosed());
        newSnap.setClosed(isClosed);

        orderMarketSnap = newSnap;
    }

    private void onOrderRunnerChange(OrderRunnerChange orderRunnerChange) {

        RunnerId runnerId = new RunnerId(orderRunnerChange.getId(), orderRunnerChange.getHc());

        OrderMarketRunner orderMarketRunner = marketRunners.computeIfAbsent(runnerId, r -> new OrderMarketRunner(this, r));

        // update the runner
        orderMarketRunner.onOrderRunnerChange(orderRunnerChange);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public OrderMarketSnap getOrderMarketSnap() {
        return orderMarketSnap;
    }

    public String getMarketId() {
        return marketId;
    }

    @Override
    public String toString() {
        StringBuilder runnersSb = new StringBuilder(" ");
        for (OrderMarketRunner runner : marketRunners.values()) {
            runnersSb.append(runner).append(" ");
        }

        return "OrderMarket{" +
                "marketRunners=" + runnersSb.toString() +
                ", marketId='" + marketId + '\'' +
                '}';
    }
}