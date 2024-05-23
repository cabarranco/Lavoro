package com.asbresearch.metrics.metrics.models.bigquery;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This is a grand angle MI level dedicated to capture key analytics that can be quickly digested by the User to
 * understand the trading performance on a given day.
 * Every day a new record should be added, and these data can be captured in BigQuery.
 */
public class DailySupervisoryMI {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * This refers to MI date (the trading day the MI is for) with YYYY-MM-DD convention.
     */
    private String date = dateFormat.format(new Date());
//    private String date = "2020-06-07";

    /**
     * Total availableToBet on Betfair at end of trading day and start of subsequent trading day (number with 2 decimal
     * points).
     * This number can be potentially retrieved from account_balance table in BQ.
     */
    private Double balance;

    /**
     * Abbreviated with ROC, this represents the daily return on capital available (aka. Balance).
     * This is a percentage (final value needs to be multiplied by 100) and calculated as
     * [ (balance – previous day Balance) / previous day balance ].
     * This number can be either positive or negative. If negative a minus sign (-) needs to be included.
     */
    private Double returnOnCapital;

    /**
     * Abbreviated with ROI, this represents the return generated on the balance concretely used during the last trading
     * day. This is a percentage (final value needs to be multiplied by 100) and can be either positive or negative.
     * If negative a minus sign (-) needs to be included.
     * It is calculated as [ (balance – previous day Balance) / Investment ]. The variable Investment is computed
     * summing all orders’ investments executed during last day – for back orders, the betAmount is considered. For lay
     * orders, the betAmount * betPrice is considered.
     */
    private Double returnOnInvestment;

    /**
     * Percentage abbreviated with BUR (final value needs to be multiplied by 100). This indicates how much of the
     * balance Pulse has been investing in last trading day. It is computed dividing the Investment variable seen above
     * by previous day Balance.
     */
    private Double balanceUtilisationRate;

    /**
     * It is (balance – previous day Balance) and represents the daily profit amount. It can be negative and in that
     * case a minus sign should be kept.
     */
    private Double totalNetProfit;

    /**
     * This represents the total daily profit gross of commission on orders executed. To compute this number a request
     * to Betfair to listClearedOrders endpoint is needed. The request will need to have the value GroupBy = BET and
     * BetStatus = SETTLED as well as trading period of last trading day. All profit values gathered then needs to be
     * algebraically summed up.
     */
    private Double totalGrossProfit;

    /**
     * This represents the profit gross of commission on orders executed. To compute this number a request to Betfair to
     * listClearedOrders endpoint is needed. The request will need to have the value GroupBy = BET and
     * BetStatus = SETTLED as well as trading period of last trading day. If betOutcome = WIN then profit needs to be
     * selected. All profits gathered then needs to be summed up.
     */
    private Double grossProfit;

    /**
     * This represents the loss gross of commission on orders executed. To compute this number a request to Betfair to
     * listClearedOrders endpoint is needed. The request will need to have the value GroupBy = BET and
     * BetStatus = SETTLED as well as trading period of last trading day.
     * If betOutcome = LOSE then profit needs to be selected. All profits gathered then needs to be summed up.
     */
    private Double grossLoss;

    /**
     * It represents grossProfit divided by totalGrossProfit. This is a percentage (final value needs to be multiplied
     * by 100) and can be either positive or negative. If negative a minus sign (-) needs to be included.
     */
    private Double grossProfitFactor;

    /**
     * This is the total number of trades executed during last trading day. The simplest way to compute this number is
     * selecting the unique number of opportunityId for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets
     */
    private Integer tradesNumber;

    /**
     * For each opportunityId in last trading day in asbanalytics.pulse_reporting.orders_bets select all related betId.
     * If at least one betId of an opportunityId is found with BetStatus = SETTLED and betOutcome = WIN in Betfair
     * endpoint listClearedOrders, then that opportunity is profitable. The number of opportunities matching these
     * criteria are then divided by tradesNumber and multiplied by 100 (it cannot be negative).
     */
    private Double tradesProfitableRate;

    /**
     * For each opportunityId in last trading day in asbanalytics.pulse_reporting.orders_bets select all related betId.
     * If at least one betId of an opportunityId is found with BetStatus = SETTLED and betOutcome = LOSE in Betfair
     * endpoint listClearedOrders, then that opportunity is profitable. The number of opportunities matching these
     * criteria are exactly what to insert in tradesLosing.
     */
    private Integer tradesWinning;

    /**
     * For each opportunityId in last trading day in asbanalytics.pulse_reporting.orders_bets select all related betId.
     * If at least one betId of an opportunityId is found with BetStatus = SETTLED and betOutcome = LOSE in Betfair
     * endpoint listClearedOrders, then that opportunity is profitable. The number of opportunities matching these
     * criteria are exactly what to insert in tradesLosing.
     */
    private Integer tradesLosing;

    /**
     * This is computed as totalNetProfit divided by tradesNumber.
     */
    private Double averageTradeNetProfit;

    /**
     * Not used yet.
     */
    private Double largestWinningTradeProfit;

    /**
     * Not used yet.
     */
    private Double largestLosingTradeLoss;

    /**
     * This is the total number of orders executed during last trading day. The simplest way to compute this number is
     * selecting the unique number of records for last trading day from table asbanalytics.pulse_reporting.orders_bets
     */
    private Integer ordersNumber;

    /**
     * This is the percentage of orders failed executed during last trading day. The simplest way to compute this number
     * is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where orderStatus = FAILURE. This number is then divided by ordersNumber
     * and multiplied by 100.
     */
    private Double ordersFailedRate;

    /**
     * This is the percentage of fully matched orders executed during last trading day. The simplest way to compute this
     * number is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where orderStatus = SUCCESS and orderAllocation = betAmount and
     * orderPrice = betPrice. This number is then divided by ordersNumber and multiplied by 100 (it cannot be negative).
     */
    private Double ordersFullyMatchedRate;

    /**
     * This is the percentage of orders best matched executed during last trading day. The simplest way to compute this
     * number is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where orderStatus = SUCCESS and orderAllocation = betAmount and:
     * • when orderSide = BACK, betPrice > orderPrice
     * • when orderSide = LAY, betPrice < orderPrice
     * This number of orders matching these criteria is then divided by ordersNumber and multiplied by 100
     * (it cannot be negative).
     */
    private Double ordersBestMatchedRate;

    /**
     * This is the percentage of orders worst matched executed during last trading day. The simplest way to compute this
     * number is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where orderStatus = SUCCESS and orderAllocation = betAmount and:
     * • when orderSide = BACK, betPrice < orderPrice
     * • when orderSide = LAY, betPrice > orderPrice
     * This number of orders matching these criteria is then divided by ordersNumber and multiplied by 100
     * (it cannot be negative).
     */
    private Double ordersWorstMatchedRate;

    /**
     * This is the percentage of orders partially matched executed during last trading day. The simplest way to compute
     * this number is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where orderStatus = SUCCESS and orderAllocation < betAmount.
     * This number of orders matching these criteria is then divided by ordersNumber and multiplied by 100
     * (it cannot be negative).
     */
    private Double ordersPartiallyMatchedRate;

    /**
     * This is the percentage of orders executed inPlay during last trading day. The simplest way to compute this number
     * is selecting the unique number of records for last trading day from table
     * asbanalytics.pulse_reporting.orders_bets where inPlay = true. This number is then divided by ordersNumber and
     * multiplied by 100 (it cannot be negative).
     */
    private Double ordersInPlayRate;

    private Integer eventsAvailable;

    private Double eventsTradedRate;

    private Integer strategiesAvailable;

    private Double strategiesTradedRate;

    private Double eventLimit;

    private Double strategyLimit;

    private Double eventStrategyLimit;

    private Double tradeMaxAllocationSum;

    // GETTER AND SETTERS


    public String getDate() {
        return date;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getReturnOnCapital() {
        return returnOnCapital;
    }

    public void setReturnOnCapital(Double previousDayBalance) {
        if (this.balance != null) {

            Double roc = ((this.balance - previousDayBalance) / previousDayBalance) * 100;
            this.returnOnCapital = Double.isInfinite(roc) || Double.isNaN(roc)
                    ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", roc));

        } else this.returnOnCapital = null;
    }

    public Double getReturnOnInvestment() {
        return returnOnInvestment;
    }

    public void setRoi(Double previousDayBalance, Double investment) {

        if (this.balance != null) {

            double roi = ((this.balance - previousDayBalance) / investment) * 100;
            this.returnOnInvestment = Double.isInfinite(roi) || Double.isNaN(roi)
                    ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", roi));

        } else this.returnOnInvestment = null;
    }

    public Double getBalanceUtilisationRate() {
        return balanceUtilisationRate;
    }

    public void setBur(Double previousDayBalance, Double investment) {
        if (this.balance != null) {

            double bur = (investment / previousDayBalance) * 100;
            this.balanceUtilisationRate = Double.isInfinite(bur) || Double.isNaN(bur)
                    ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", bur));

        } else this.balanceUtilisationRate = null;
    }

    public Double getTotalNetProfit() {
        return totalNetProfit;
    }

    public void setTotalNetProfit(Double previousDayBalance) {
        if (this.balance != null) {
            this.totalNetProfit = Double.parseDouble(String.format(Locale.UK,"%.2f", this.balance - previousDayBalance));
        } else this.totalNetProfit = null;
    }

    public Double getTotalGrossProfit() {
        return totalGrossProfit;
    }

    public void setTotalGrossProfit(Double totalGrossProfit) {
        this.totalGrossProfit = Double.isNaN(totalGrossProfit) || Double.isInfinite(totalGrossProfit)
                ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", totalGrossProfit));
    }

    public Double getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(Double grossProfit) {
        this.grossProfit = Double.isNaN(grossProfit) || Double.isInfinite(grossProfit)
                ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", grossProfit));
    }

    public Double getGrossLoss() {
        return grossLoss;
    }

    public void setGrossLoss(Double grossLoss) {
        this.grossLoss = Double.parseDouble(String.format(Locale.UK,"%.2f", grossLoss));
    }

    public Double getGrossProfitFactor() {
        return grossProfitFactor;
    }

    public void setGrossProfitFactor(Double grossProfitFactor) {
        this.grossProfitFactor = Double.isNaN(grossProfitFactor) || Double.isInfinite(grossProfitFactor)
                ? null : Double.parseDouble(
                String.format(Locale.UK,"%.2f",grossProfitFactor)
        );
    }

    public Integer getTradesNumber() {
        return tradesNumber;
    }

    public void setTradesNumber(Integer tradesNumber) {
        this.tradesNumber = tradesNumber;
    }

    public Double getTradesProfitableRate() {
        return tradesProfitableRate;
    }

    public void setTradesProfitableRate(float tradesProfitableRate) {
        this.tradesProfitableRate = Double.isNaN(tradesProfitableRate) || Double.isInfinite(tradesProfitableRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK,"%.2f", tradesProfitableRate * 100)
        );
    }

    public Integer getTradesWinning() {
        return tradesWinning;
    }

    public void setTradesWinning(Integer tradesWinning) {
        this.tradesWinning = tradesWinning;
    }

    public Double getAverageTradeNetProfit() {
        return averageTradeNetProfit;
    }

    public void setAverageTradeNetProfit(Double averageTradeNetProfit) {
        this.averageTradeNetProfit = Double.isNaN(averageTradeNetProfit) || Double.isInfinite(averageTradeNetProfit)
                ? null : Double.parseDouble(
                String.format(Locale.UK,"%.2f", averageTradeNetProfit)
        );;
    }

    public Double getLargestWinningTradeProfit() {
        return largestWinningTradeProfit;
    }

    public void setLargestWinningTradeProfit(Double largestWinningTradeProfit) {
        this.largestWinningTradeProfit = largestWinningTradeProfit;
    }

    public Double getLargestLosingTradeLoss() {
        return largestLosingTradeLoss;
    }

    public void setLargestLosingTradeLoss(Double largestLosingTradeLoss) {
        this.largestLosingTradeLoss = largestLosingTradeLoss;
    }

    public Integer getOrdersNumber() {
        return ordersNumber;
    }

    public void setOrdersNumber(Integer ordersNumber) {
        this.ordersNumber = ordersNumber;
    }

    public Double getOrdersFailedRate() {
        return ordersFailedRate;
    }

    public void setOrdersFailedRate(float ordersFailedRate) {
        this.ordersFailedRate = Double.isNaN(ordersFailedRate) || Double.isInfinite(ordersFailedRate)
                ? null : Double.parseDouble(
                        String.format(Locale.UK,"%.2f", (ordersFailedRate) * 100)
        );
    }

    public Double getOrdersFullyMatchedRate() {
        return ordersFullyMatchedRate;
    }

    public void setOrdersFullyMatchedRate(float ordersFullyMatchedRate) {
        this.ordersFullyMatchedRate = Double.isNaN(ordersFullyMatchedRate) || Double.isInfinite(ordersFullyMatchedRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK, "%.2f", (ordersFullyMatchedRate) * 100)
        );
    }

    public Double getOrdersBestMatchedRate() {
        return ordersBestMatchedRate;
    }

    public void setOrdersBestMatchedRate(float ordersBestMatchedRate) {
        this.ordersBestMatchedRate = Double.isNaN(ordersBestMatchedRate) || Double.isInfinite(ordersBestMatchedRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK, "%.2f", (ordersBestMatchedRate) * 100)
        );
    }

    public Double getOrdersWorstMatchedRate() {
        return ordersWorstMatchedRate;
    }

    public void setOrdersWorstMatchedRate(float ordersWorstMatchedRate) {
        this.ordersWorstMatchedRate = Double.isNaN(ordersWorstMatchedRate) || Double.isInfinite(ordersWorstMatchedRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK, "%.2f", (ordersWorstMatchedRate) * 100)
        );
    }

    public Double getOrdersPartiallyMatchedRate() {
        return ordersPartiallyMatchedRate;
    }

    public void setOrdersPartiallyMatchedRate(float ordersPartiallyMatchedRate) {
        this.ordersPartiallyMatchedRate = Double.isNaN(ordersPartiallyMatchedRate) || Double.isInfinite(ordersPartiallyMatchedRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK, "%.2f", (ordersPartiallyMatchedRate) * 100)
        );
    }

    public Double getOrdersInPlayRate() {
        return ordersInPlayRate;
    }

    public void setOrdersInPlayRate(float ordersInPlayRate) {
        this.ordersInPlayRate = Double.isNaN(ordersInPlayRate) || Double.isInfinite(ordersInPlayRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK, "%.2f", ordersInPlayRate * 100)
        );
    }

    public Integer getEventsAvailable() {
        return eventsAvailable;
    }

    public void setEventsAvailable(Integer eventsAvailable) {
        this.eventsAvailable = eventsAvailable;
    }

    public Double getEventsTradedRate() {
        return eventsTradedRate;
    }

    public void setEventsTradedRate(float eventsTradedRate) {
        this.eventsTradedRate = Double.isNaN(eventsTradedRate) || Double.isInfinite(eventsTradedRate)
                ? null : Double.parseDouble(
                String.format(Locale.UK,"%.2f",eventsTradedRate * 100)
        );
    }

    public Integer getStrategiesAvailable() {
        return strategiesAvailable;
    }

    public void setStrategiesAvailable(Integer strategiesAvailable) {
        this.strategiesAvailable = strategiesAvailable;
    }

    public Double getStrategiesTradedRate() {
        return strategiesTradedRate;
    }

    public void setStrategiesTradedRate(Double strategiesTradedRate) {
        this.strategiesTradedRate = strategiesTradedRate;
    }

    public Double getEventLimit() {
        return eventLimit;
    }

    public void setEventLimit(Double eventLimit) {
        this.eventLimit = eventLimit;
    }

    public Double getStrategyLimit() {
        return strategyLimit;
    }

    public void setStrategyLimit(Double strategyLimit) {
        this.strategyLimit = strategyLimit;
    }

    public Double getEventStrategyLimit() {
        return eventStrategyLimit;
    }

    public void setEventStrategyLimit(Double eventStrategyLimit) {
        this.eventStrategyLimit = eventStrategyLimit;
    }

    public Double getTradeMaxAllocationSum() {
        return tradeMaxAllocationSum;
    }

    public void setTradeMaxAllocationSum(Double tradeMaxAllocationSum) {
        this.tradeMaxAllocationSum = tradeMaxAllocationSum;
    }

    public Integer getTradesLosing() {
        return tradesLosing;
    }

    public void setTradesLosing(Integer tradesLosing) {
        this.tradesLosing = tradesLosing;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
