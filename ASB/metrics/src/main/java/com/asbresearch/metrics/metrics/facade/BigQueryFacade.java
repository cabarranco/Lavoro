package com.asbresearch.metrics.metrics.facade;

import com.asbresearch.metrics.metrics.models.betfair.ClearedOrder;
import com.asbresearch.metrics.metrics.models.betfair.ClearedOrders;
import com.asbresearch.metrics.metrics.services.BigQueryService;
import com.google.cloud.bigquery.FieldValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class BigQueryFacade {

    private static final Logger log = LoggerFactory.getLogger(BigQueryFacade.class);
    private int tradesWinning = 0;
    private int tradesLosing = 0;
    private final LocalDate today;
    private final LocalDate yesterday;
    private final LocalDate tomorrow;
    private HashMap<String, Double> wonOpportunityProfit = new HashMap<>();
    private HashMap<String, Double> lostOpportunityProfit = new HashMap<>();

    @Autowired
    private BigQueryService bigQueryService;

    public BigQueryFacade() {

        this.today = LocalDate.now();
//        this.today = LocalDate.parse("2020-06-07");
        this.yesterday = today.minusDays(1);
        this.tomorrow = today.plusDays(1);

    }

    private double getBalance(LocalDate start, LocalDate end) {

        String queryBalance = String.format(
                "SELECT availableToBet FROM `asbanalytics.pulse_reporting.account_balance` where datetime > \"%d-%d-%dT04:00:00\" AND datetime < \"%d-%d-%dT03:59:59\" order by dateTime limit 1",
                start.getYear(),
                start.getMonthValue(),
                start.getDayOfMonth(),
                end.getYear(),
                end.getMonthValue(),
                end.getDayOfMonth()
        );

        try {
            Iterable<FieldValueList> iterable = bigQueryService.query(queryBalance);

            FieldValueList values = iterable.iterator().next();
            return values.get(0).getDoubleValue();
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0d;

    }

    public double getTodayBalance() {
        return getBalance(this.today, this.tomorrow);
    }

    public double getYesterdayBalance() {
        return getBalance(this.yesterday, this.today);
    }

    public List<String> getStrategiesIds() {

        String queryDistinctStrategyId = String.format(
                "SELECT DISTINCT strategyId FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        List<String> ids = new ArrayList<>();

        try {
            Iterable<FieldValueList> iterableStrategiesIds = bigQueryService.query(queryDistinctStrategyId);

            for (FieldValueList values : iterableStrategiesIds) {
                ids.add(values.get(0).getStringValue());
            }
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No ");
        }

        return ids;
    }

    public List<String> getBetIds(String strategyId) {

        String queryBetIds = String.format(
                "SELECT betId FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and strategyId = \"%s\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth(),
                strategyId
        );

        List<String> ids = new ArrayList<>();

        try {
            Iterable<FieldValueList> iterableStrategiesIds = bigQueryService.query(queryBetIds);

            for (FieldValueList values : iterableStrategiesIds) {
                ids.add(values.get(0).getStringValue());
            }
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No bet ids found for strategy id: " + strategyId);
        }

        return ids;
    }

    public double getSumBackBetAmount() {
        return getSumBackBetAmount(null);
    }

    public double getSumBackBetAmount(String strategyId) {

        String querySumBackBetAmount = String.format(
                "SELECT SUM(betAmount) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderSide = \"BACK\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            querySumBackBetAmount += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableBack = bigQueryService.query(querySumBackBetAmount);

            FieldValueList valuesBack = iterableBack.iterator().next();
            return !valuesBack.get(0).isNull() ? valuesBack.get(0).getDoubleValue() : 0d;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0d;
    }

    public double getSumLayBetAmount() {
        return getSumLayBetAmount(null);
    }

    public double getSumLayBetAmount(String strategyId) {

        String querySumLayBetAmount = String.format(
                "SELECT SUM(betAmount * betPrice) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderSide = \"LAY\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            querySumLayBetAmount += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableLay = bigQueryService.query(querySumLayBetAmount);

            FieldValueList valuesLay = iterableLay.iterator().next();
            return !valuesLay.get(0).isNull() ? valuesLay.get(0).getDoubleValue() : 0d;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0d;
    }

    public int getTradesNumbers() {
        return getTradesNumbers(null);
    }

    public int getTradesNumbers(String strategyId) {

        String queryTradesNumber = String.format(
                "SELECT COUNT(DISTINCT opportunityId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            queryTradesNumber += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableTradesNumbers = bigQueryService.query(queryTradesNumber);

            FieldValueList valuesTradesNumbers = iterableTradesNumbers.iterator().next();
            return  !valuesTradesNumbers.get(0).isNull() ? valuesTradesNumbers.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public void calculateTradesWinningLosing(ClearedOrders clearedOrders) {
        calculateTradesWinningLosing(null, clearedOrders);
    }

    public void calculateTradesWinningLosing(String strategyId, ClearedOrders clearedOrders) {

        String queryOpportunitiesBetIds = String.format(
                "SELECT opportunityId, betId FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" %s order by opportunityId",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth(),
                (strategyId != null && !strategyId.isEmpty()) ? "and strategyId = \"" + strategyId + "\"" : ""
        );

        tradesLosing = 0;
        tradesWinning = 0;

        try {
            Iterable<FieldValueList> iterableTradesNumbers = bigQueryService.query(queryOpportunitiesBetIds);
            HashMap<String, List<String>> opIdBetIdMap = new HashMap<>();

            for (FieldValueList values : iterableTradesNumbers) {

                opIdBetIdMap.computeIfAbsent(values.get(0).getStringValue(), k -> new ArrayList<>());
                wonOpportunityProfit.putIfAbsent(values.get(0).getStringValue(), 0d);
                lostOpportunityProfit.putIfAbsent(values.get(0).getStringValue(), 0d);

                opIdBetIdMap.get(values.get(0).getStringValue()).add(values.get(1).getStringValue());
            }

            for (Map.Entry<String, List<String>> entry : opIdBetIdMap.entrySet()) {
                boolean winning = false;

                for (String betId : entry.getValue()) {
                    ClearedOrder clearedOrder = clearedOrders.getClearedOrders().stream()
                            .filter(co -> co.getBetId().equals(betId))
                            .findFirst().orElse(null);

                    if (clearedOrder != null && "WON".equalsIgnoreCase(clearedOrder.getBetOutcome())) {
                        winning = true;
                        double profit = wonOpportunityProfit.get(entry.getKey());
                        wonOpportunityProfit.put(entry.getKey(), profit + clearedOrder.getProfit());
                    } else if (clearedOrder != null) {
                        double profit = lostOpportunityProfit.get(entry.getKey());
                        lostOpportunityProfit.put(entry.getKey(), profit + Math.abs(clearedOrder.getProfit()));
                    }
                }

                if (winning) tradesWinning++;
                if (!winning) tradesLosing++;
            }
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }
    }

    public int getTradesWinning() {
        return tradesWinning;
    }

    public int getTradesLosing() {
        return tradesLosing;
    }

    public int getFailedOrders() {
        return getFailedOrders(null);
    }

    public int getFailedOrders(String strategyId) {

        String query = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"FAILURE\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            query += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableFailedOrders = bigQueryService.query(query);

            FieldValueList valuesFailedOrders = iterableFailedOrders.iterator().next();
            return !valuesFailedOrders.get(0).isNull() ? valuesFailedOrders.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getFullyMatched() {
        return getFullyMatched(null);
    }

    public int getFullyMatched(String strategyId) {

        String query = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"SUCCESS\" and orderAllocation = betAmount and orderPrice = betPrice",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            query += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableFullyMatched = bigQueryService.query(query);

            FieldValueList valuesFullyMatched = iterableFullyMatched.iterator().next();
            return !valuesFullyMatched.get(0).isNull() ? valuesFullyMatched.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getBestMatched() {
        return getBestMatched(null);
    }

    public int getBestMatched(String strategyId) {

        String queryBestMatchedRate = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"SUCCESS\" and orderAllocation = betAmount and ((orderSide = \"BACK\" and betPrice > orderPrice) or (orderSide = \"LAY\" and betPrice < orderPrice))",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            queryBestMatchedRate += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableestMatchedRate = bigQueryService.query(queryBestMatchedRate);

            FieldValueList valuesestMatchedRate = iterableestMatchedRate.iterator().next();
            return !valuesestMatchedRate.get(0).isNull() ? valuesestMatchedRate.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getWorstMatched() {
        return getWorstMatched(null);
    }

    public int getWorstMatched(String strategyId) {

        String queryWorstMatchedRate = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"SUCCESS\" and orderAllocation = betAmount and ((orderSide = \"BACK\" and betPrice < orderPrice) or (orderSide = \"LAY\" and betPrice > orderPrice))",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            queryWorstMatchedRate += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableWorstMatchedRate = bigQueryService.query(queryWorstMatchedRate);

            FieldValueList valuesWorstMatchedRate = iterableWorstMatchedRate.iterator().next();
            return !valuesWorstMatchedRate.get(0).isNull() ? valuesWorstMatchedRate.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getPartiallyMatched() {
        return getPartiallyMatched(null);
    }

    public int getPartiallyMatched(String strategyId) {

        String queryPartiallyMatchedRate = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"SUCCESS\" and orderAllocation < betAmount",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            queryPartiallyMatchedRate += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterablePartiallyMatchedRate = bigQueryService.query(queryPartiallyMatchedRate);

            FieldValueList valuesPartiallyMatchedRate = iterablePartiallyMatchedRate.iterator().next();
            return !valuesPartiallyMatchedRate.get(0).isNull() ? valuesPartiallyMatchedRate.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getInPlay() {
        return getInPlay(null);
    }

    public int getInPlay(String strategyId) {

        String queryInPlayRate = String.format(
                "SELECT COUNT(betId) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimeStamp > \"%d-%d-%dT04:00:00\" and orderTimeStamp < \"%d-%d-%dT03:59:59\" and orderStatus = \"SUCCESS\" and inPlay = true",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        if (strategyId != null && !strategyId.isEmpty())
            queryInPlayRate += "and strategyId = \"" + strategyId + "\"";

        try {
            Iterable<FieldValueList> iterableInPlayRate = bigQueryService.query(queryInPlayRate);

            FieldValueList valuesInPlayRate = iterableInPlayRate.iterator().next();
            return !valuesInPlayRate.get(0).isNull() ? valuesInPlayRate.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No balance found for the current day");
        }

        return 0;
    }

    public int getEventsAvailable() {

        String queryEventsAvailable = String.format(
                "SELECT COUNT(DISTINCT(eventId)) FROM `asbanalytics.betstore.betfair_market_catalogue` where startTime > \"%d-%d-%dT04:00:00\" and startTime < \"%d-%d-%dT03:59:59\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        try {
            Iterable<FieldValueList> iterableEventsAvailable = bigQueryService.query(queryEventsAvailable);

            FieldValueList valueEventsAvailable = iterableEventsAvailable.iterator().next();
            return !valueEventsAvailable.get(0).isNull() ? valueEventsAvailable.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No events found for the current day");
        }

        return 0;
    }

    public int getEventsWithTrade() {

        String queryEventsWithTrade = String.format(
                "SELECT COUNT(DISTINCT(eventId)) FROM `asbanalytics.pulse_reporting.orders_bets` where orderTimestamp > \"%d-%d-%dT04:00:00\" and orderTimestamp < \"%d-%d-%dT03:59:59\"",
                this.yesterday.getYear(),
                this.yesterday.getMonthValue(),
                this.yesterday.getDayOfMonth(),
                this.today.getYear(),
                this.today.getMonthValue(),
                this.today.getDayOfMonth()
        );

        try {
            Iterable<FieldValueList> iterableEventsWithTrade = bigQueryService.query(queryEventsWithTrade);

            FieldValueList valueEventsWithTrade = iterableEventsWithTrade.iterator().next();
            return !valueEventsWithTrade.get(0).isNull() ? valueEventsWithTrade.get(0).getNumericValue().intValue() : 0;
        } catch (InterruptedException | NoSuchElementException e) {
            log.error("No events found for the current day");
        }

        return 0;
    }

    public double getWonOrdersProfit() {

        double maxEntry = 0;

        for (Map.Entry<String, Double> entry : this.wonOpportunityProfit.entrySet()) {
            if (entry.getValue().compareTo(maxEntry) > 0) {
                maxEntry = entry.getValue();
            }
        }

        return maxEntry;
    }

    public double getLostOrdersProfit() {

        double maxEntry = 0;

        for (Map.Entry<String, Double> entry : this.lostOpportunityProfit.entrySet()) {
            if (entry.getValue().compareTo(maxEntry) > 0) {
                maxEntry = entry.getValue();
            }
        }

        return maxEntry;
    }
}
