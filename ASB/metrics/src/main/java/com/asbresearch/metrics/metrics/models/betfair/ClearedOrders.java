package com.asbresearch.metrics.metrics.models.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClearedOrders {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private List<ClearedOrder> clearedOrders;

    public List<ClearedOrder> getClearedOrders() {
        return clearedOrders;
    }

    public void setClearedOrders(List<ClearedOrder> clearedOrders) {
        this.clearedOrders = clearedOrders;
    }

    public double getTotalGrossProfit() {

        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            sum += clearedOrder.getProfit();
        }

        return sum;
    }

    public Double getTotalGrossProfit(List<String> betIds) {
        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            if (betIds.contains(clearedOrder.getBetId()))
                sum += clearedOrder.getProfit();
        }

        return sum;
    }

    public double getGrossProfit() {

        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            if ("WON".equalsIgnoreCase(clearedOrder.getBetOutcome()))
                sum += clearedOrder.getProfit();
        }

        return sum;
    }

    public double getGrossProfit(List<String> betIds) {

        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            if ("WON".equalsIgnoreCase(clearedOrder.getBetOutcome()) && betIds.contains(clearedOrder.getBetId()))
                sum += clearedOrder.getProfit();
        }

        return sum;
    }

    public double getGrossLoss() {

        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            if ("LOST".equalsIgnoreCase(clearedOrder.getBetOutcome()))
                sum += clearedOrder.getProfit();
        }

        return Math.abs(sum);
    }

    public double getGrossLoss(List<String> betIds) {

        double sum = 0d;

        for (ClearedOrder clearedOrder : this.clearedOrders) {
            if ("LOST".equalsIgnoreCase(clearedOrder.getBetOutcome()) && betIds.contains(clearedOrder.getBetId()))
                sum += clearedOrder.getProfit();
        }

        return Math.abs(sum);
    }


    public ClearedOrders filter(List<String> betIds) {

        List<ClearedOrder> clearedOrderList = this.clearedOrders.stream()
                .filter(co -> betIds.contains(co.getBetId())).collect(Collectors.toList());

        ClearedOrders clearedOrders = new ClearedOrders();
        clearedOrders.setClearedOrders(clearedOrderList);

        return clearedOrders;
    }

    public void filterByDate() {

        List<ClearedOrder> filtered = new ArrayList<>();

        for (ClearedOrder clearedOrder : this.clearedOrders) {

            try {
                LocalDateTime yeasterday = LocalDate.now().atTime(3, 59, 59).minusDays(1);
                String stringDate = clearedOrder.getPlacedDate()
                        .replace("T", " ").replace("Z", "");

                LocalDateTime date = dateFormat.parse(stringDate)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                if (date.isAfter(yeasterday)) {
                    filtered.add(clearedOrder);
                }
            } catch (ParseException ignore) {}
        }

        this.clearedOrders.clear();
        this.clearedOrders = new ArrayList<>(filtered);
    }
}
