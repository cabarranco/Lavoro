package com.asbresearch.pulse.model;

import lombok.Data;
import java.time.Instant;
import static com.asbresearch.common.BigQueryUtil.csvValue;

@Data
public class SimOrder {
    private Instant orderTimeStamp;
    private String venue = "Betfair";
    private String bookRunner;
    private String marketId;
    private String orderSide;
    private Double orderAllocation;
    private String orderAllocationCurrency = "GBP";
    private Double orderPrice;
    private String orderType;
    private String orderId;
    private Long selectionId;
    private String eventId;
    private String opportunityId;
    private String strategyId;
    private String eventName;
    private Boolean inPlay;
    private String node;
    private Double pl;

    public String toCsvData() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(orderTimeStamp),
                csvValue(venue),
                csvValue(bookRunner),
                csvValue(marketId),
                csvValue(orderSide),
                csvValue(orderAllocation),
                csvValue(orderAllocationCurrency),
                csvValue(orderPrice),
                csvValue(orderType),
                csvValue(orderId),
                csvValue(selectionId),
                csvValue(eventId),
                csvValue(opportunityId),
                csvValue(strategyId),
                csvValue(eventName),
                csvValue(inPlay),
                csvValue(node),
                csvValue(pl));
    }
}
