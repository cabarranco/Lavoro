package com.asbresearch.betfair.esa.cache.market;

import com.betfair.esa.swagger.model.MarketChange;
import java.util.EventObject;
import lombok.Value;

@Value
final public class MarketChangeEvent extends EventObject {
    //the raw change message that was just applied
    private final MarketChange change;
    //the market changed - this is reference invariant
    private final Market market;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param change
     * @param market
     * @throws IllegalArgumentException if source is null.
     */
    public MarketChangeEvent(Object source, MarketChange change, Market market) {
        super(source);
        this.change = change;
        this.market = market;
    }

    public MarketSnap getSnap() {
        return market.getSnap();
    }
}
