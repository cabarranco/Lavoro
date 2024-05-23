package com.asbresearch.metrics.metrics.models.bigquery;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This is a more detailed MI level dedicated to capture key analytics split by strategy.
 * Every day multiple records should be added, and these data can be captured in BigQuery
 * asbanalytics.pulse_metrics.daily_strategies_mi
 */
public class DailyStrategiesMI {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public enum Metric {
        TOTAL_GROSS_PROFIT("totalGrossProfit"),
        GROSS_PROFIT("grossProfit"),
        GROSS_LOSS("grossLoss"),
        GROSS_PROFIT_FACTOR("grossProfitFactor"),
        TRADES_NUMBER("tradesNumber"),
        TRADES_PROFITABLE_RATE("tradesProfitableRate"),
        TRADES_WINNING("tradesWinning"),
        TRADES_LOSING("tradesLosing"),
        LARGEST_WINNING_TRADE_PROFIT("largestWinningTradeProfit"),
        LARGEST_LOSING_TRADE_LOSS("largestLosingTradeLoss");

        private final String value;

        Metric(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public DailyStrategiesMI(String metric, String strategyId, Double metricValue) {
        this.metric = metric;
        this.strategyId = strategyId;
        this.metricValue = Double.isInfinite(metricValue) || Double.isNaN(metricValue)
                ? null : Double.parseDouble(String.format(Locale.UK,"%.2f", metricValue));
    }

    /**
     * This refers to MI date (the trading day the MI is for) with YYYY-MM-DD convention.
     */
    private String date = dateFormat.format(new Date());
//    private String date = "2020-06-07";

    /**
     * This field can assume various values for which the computation can be inferred from paragraph above considering
     * only the split by strategyId: {@link Metric}
     */
    private String metric;

    /**
     * Strategy by which the metric above is grouped.
     */
    private String strategyId;

    /**
     * Value of the metric in reference to field metric and strategyId.
     */
    private Double metricValue;

    // GETTER AND SETTERS

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
