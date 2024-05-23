package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.MarketBettingType;
import com.asbresearch.betfair.ref.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
public class MarketFilter {
    private String textQuery;
    private Set<String> exchangeIds;
    private Set<String> eventTypeIds;
    private Set<String> marketIds;
    private Boolean inPlayOnly;
    private Set<String> eventIds;
    private Set<String> competitionIds;
    private Set<String> venues;
    private Boolean bspOnly;
    private Boolean turnInPlayEnabled;
    private Set<MarketBettingType> marketBettingTypes;
    private Set<String> marketCountries;
    private Set<String> marketTypeCodes;
    private TimeRange marketStartTime;
    private Set<OrderStatus> withOrders;
}
