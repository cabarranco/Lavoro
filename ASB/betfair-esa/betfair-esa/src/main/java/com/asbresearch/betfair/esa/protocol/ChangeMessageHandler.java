package com.asbresearch.betfair.esa.protocol;

import com.betfair.esa.swagger.model.MarketChange;
import com.betfair.esa.swagger.model.OrderMarketChange;
import com.betfair.esa.swagger.model.StatusMessage;

public interface ChangeMessageHandler {
    void onOrderChange(ChangeMessage<OrderMarketChange> change);

    void onMarketChange(ChangeMessage<MarketChange> change);

    void onErrorStatusNotification(StatusMessage message);
}
