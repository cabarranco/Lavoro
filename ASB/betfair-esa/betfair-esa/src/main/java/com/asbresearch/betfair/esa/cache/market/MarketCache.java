package com.asbresearch.betfair.esa.cache.market;

import com.asbresearch.betfair.esa.protocol.ChangeMessage;
import com.betfair.esa.swagger.model.MarketChange;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.stream.Collectors.toList;

/**
 * Thread safe cache of markets
 */
@Slf4j
public class MarketCache {
    private Map<String, Market> markets = new ConcurrentHashMap<>();
    //whether markets are automatically removed on close (default is True)
    private boolean isMarketRemovedOnClose;
    //conflation indicates slow consumption
    private int conflatedCount;

    private CopyOnWriteArrayList<MarketChangeListener> marketChangeListeners = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BatchMarketsChangeListener> batchMarketChangeListeners = new CopyOnWriteArrayList<>();

    public MarketCache() {
        this.isMarketRemovedOnClose = true;
    }

    public void onMarketChange(ChangeMessage<MarketChange> changeMessage) {
        log.debug("MarketChange-> publishTime={} arrivalTime={} id={}", changeMessage.getPublishTime(), changeMessage.getArrivalTime(),
                changeMessage.getItems().stream().map(marketChange -> marketChange.getId()).collect(toList()));
        if (changeMessage.isStartOfNewSubscription()) {
            if (changeMessage.getItems() != null) {
                changeMessage.getItems().forEach(marketChange -> markets.remove(marketChange.getId()));
            }
        }
        if (changeMessage.getItems() != null) {
            //lazy build events
            List<MarketChangeEvent> batch = (batchMarketChangeListeners.size() == 0) ? null : new ArrayList<>(changeMessage.getItems().size());
            for (MarketChange marketChange : changeMessage.getItems()) {
                Market market = onMarketChange(marketChange, Instant.ofEpochMilli(changeMessage.getPublishTime()));
                if (isMarketRemovedOnClose && market.isClosed()) {
                    //remove on close
                    markets.remove(market.getMarketId());
                }
                //lazy build events
                if (batch != null || marketChangeListeners.size() != 0) {
                    MarketChangeEvent marketChangeEvent = new MarketChangeEvent(this, marketChange, market);
                    if (marketChangeListeners != null) {
                        dispatchMarketChanged(marketChangeEvent);
                    }
                    if (batch != null) {
                        batch.add(marketChangeEvent);
                    }
                }
            }
            if (batch != null) {
                dispatchBatchMarketChanged(new BatchMarketChangeEvent(batch));
            }
        }
    }

    private Market onMarketChange(MarketChange marketChange, Instant publishTime) {
        if (Boolean.TRUE.equals(marketChange.getCon())) {
            conflatedCount++;
        }
        Market market = markets.computeIfAbsent(marketChange.getId(), k -> new Market(k));
        market.onMarketChange(marketChange, publishTime);
        return market;
    }

    public int getConflatedCount() {
        return conflatedCount;
    }

    void setConflatedCount(int conflatedCount) {
        this.conflatedCount = conflatedCount;
    }

    public boolean isMarketRemovedOnClose() {
        return isMarketRemovedOnClose;
    }

    public void setMarketRemovedOnClose(boolean marketRemovedOnClose) {
        isMarketRemovedOnClose = marketRemovedOnClose;
    }

    public Market getMarket(String marketId) {
        //queries by market id - the result is invariant for the lifetime of the market.
        return markets.get(marketId);
    }

    public Iterable<Market> getMarkets() {
        //all the cached markets
        return markets.values();
    }

    public int getCount() {
        //market count
        return markets.size();
    }

    // Event for each market change

    private void dispatchMarketChanged(MarketChangeEvent marketChangeEvent) {
        try {
            marketChangeListeners.forEach(l -> l.marketChange(marketChangeEvent));
        } catch (Exception e) {
            log.error("Exception from event listener", e);
        }
    }

    public void addMarketChangeListener(MarketChangeListener marketChangeListener) {
        marketChangeListeners.add(marketChangeListener);
    }

    public void removeMarketChangeListener(MarketChangeListener marketChangeListener) {
        marketChangeListeners.remove(marketChangeListener);
    }

    // Event for each batch of market changes
    // (note to be truly atomic you will want to set to merge segments otherwise an event could be segmented)

    private void dispatchBatchMarketChanged(BatchMarketChangeEvent batchMarketChangeEvent) {
        try {
            batchMarketChangeListeners.forEach(l -> l.batchMarketsChange(batchMarketChangeEvent));
        } catch (Exception e) {
            log.error("Exception from batch event listener", e);
        }
    }

    public void addBatchMarketChangeListener(BatchMarketsChangeListener batchMarketChangeListener) {
        batchMarketChangeListeners.add(batchMarketChangeListener);
    }

    public void removeBatchMarketChangeListener(BatchMarketsChangeListener batchMarketChangeListener) {
        batchMarketChangeListeners.remove(batchMarketChangeListener);
    }

    public class BatchMarketChangeEvent extends EventObject {
        private List<MarketChangeEvent> changes;

        /**
         * Constructs a prototypical Event.
         *
         * @param source The object on which the Event initially occurred.
         * @throws IllegalArgumentException if source is null.
         */
        public BatchMarketChangeEvent(Object source) {
            super(source);
        }

        public List<MarketChangeEvent> getChanges() {
            return changes;
        }

        void setChanges(List<MarketChangeEvent> changes) {
            this.changes = changes;
        }
    }

    public interface MarketChangeListener extends java.util.EventListener {
        void marketChange(MarketChangeEvent marketChangeEvent);
    }

    public interface BatchMarketsChangeListener extends java.util.EventListener {
        void batchMarketsChange(BatchMarketChangeEvent batchMarketChangeEvent);
    }
}
