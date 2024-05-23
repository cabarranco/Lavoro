package com.asbresearch.metrics.metrics.facade;

import com.asbresearch.metrics.metrics.models.betfair.ClearedOrders;
import com.asbresearch.metrics.metrics.services.BetfairRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;

@Component
public class BetfairFacade {

    private static final Logger log = LoggerFactory.getLogger(BigQueryFacade.class);
    private final LocalDate today;
    private final LocalDate yesterday;

    @Autowired
    private BetfairRestService betfairRestService;

    public BetfairFacade() {
        this.today = LocalDate.now();
//        this.today = LocalDate.parse("2020-06-07");
        this.yesterday = today.minusDays(1);
    }

    public ClearedOrders getListClearedOrders() {

        HashMap<String, Object> filters = new HashMap<>();
        HashMap<String, String> filterDate = new HashMap<>();

        filterDate.put("from", String.format(
                "%d-%d-%dT04:00:00",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth()
        ));

        filterDate.put("to", String.format(
                "%d-%d-%dT03:59:00",
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        ));

        filters.put("betStatus", "SETTLED");
        filters.put("groupBy", "BET");
        filters.put("settledDateRange", filterDate);

        ClearedOrders clearedOrders = betfairRestService.getListClearedOrders(filters);

        clearedOrders.filterByDate();

        return clearedOrders;
    }
}
