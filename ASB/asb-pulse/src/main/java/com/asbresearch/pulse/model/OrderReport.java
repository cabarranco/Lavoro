package com.asbresearch.pulse.model;

import java.time.Instant;
import lombok.Data;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Data
public class OrderReport {
    private Instant orderTimeStamp;
    private String venue;
    private String orderStatus;
    private String executionStatus;
    private String bookRunner;
    private String marketId;
    private String orderSide;
    private Double orderAllocation;
    private String orderAllocationCurrency;
    private Double orderPrice;
    private String orderType;
    private Double betAmount;
    private String betAmountCurrency;
    private Double betPrice;
    private String abortReason;
    private String betId;
    private Long selectionId;
    private String eventId;
    private String opportunityId;
    private String strategyId;
    private String eventName;
    private Boolean inPlay;
    private String node;

    public String toCsvData() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(orderTimeStamp),
                csvValue(venue),
                csvValue(orderStatus),
                csvValue(bookRunner),
                csvValue(marketId),
                csvValue(orderSide),
                csvValue(orderAllocation),
                csvValue(orderAllocationCurrency),
                csvValue(orderPrice),
                csvValue(orderType),
                csvValue(betAmount),
                csvValue(betAmountCurrency),
                csvValue(betPrice),
                csvValue(abortReason),
                csvValue(betId),
                csvValue(selectionId),
                csvValue(eventId),
                csvValue(opportunityId),
                csvValue(strategyId),
                csvValue(executionStatus),
                csvValue(eventName),
                csvValue(inPlay),
                csvValue(node));
    }
}
