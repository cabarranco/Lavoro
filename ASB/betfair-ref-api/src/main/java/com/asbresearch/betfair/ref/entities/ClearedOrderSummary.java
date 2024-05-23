package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.OrderType;
import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.asbresearch.betfair.ref.enums.Side;
import java.time.Instant;
import lombok.Value;

@Value
public class ClearedOrderSummary {
    private final String eventTypeId;
    private final String eventId;
    private final String marketId;
    private final Long selectionId;
    private final Double handicap;
    private final String betId;
    private final Instant placedDate;
    private final PersistenceType persistenceType;
    private final OrderType orderType;
    private final Side side;
    private final ItemDescription itemDescription;
}

    
