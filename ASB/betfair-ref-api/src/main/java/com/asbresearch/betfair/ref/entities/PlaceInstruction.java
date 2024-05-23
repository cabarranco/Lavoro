package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.OrderType;
import com.asbresearch.betfair.ref.enums.Side;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class PlaceInstruction {
    private final OrderType orderType;
    private final Long selectionId;
    private final double handicap;
    private final Side side;
    private final LimitOrder limitOrder;
    private final LimitOnCloseOrder limitOnCloseOrder;
    private final MarketOnCloseOrder marketOnCloseOrder;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PlaceInstruction(@JsonProperty("orderType") OrderType orderType,
                            @JsonProperty("selectionId") Long selectionId,
                            @JsonProperty("handicap") double handicap,
                            @JsonProperty("side") Side side,
                            @JsonProperty("limitOrder") LimitOrder limitOrder,
                            @JsonProperty("limitOnCloseOrder") LimitOnCloseOrder limitOnCloseOrder,
                            @JsonProperty("marketOnCloseOrder") MarketOnCloseOrder marketOnCloseOrder) {
        this.orderType = orderType;
        this.selectionId = selectionId;
        this.handicap = handicap;
        this.side = side;
        this.limitOrder = limitOrder;
        this.limitOnCloseOrder = limitOnCloseOrder;
        this.marketOnCloseOrder = marketOnCloseOrder;
    }
}
