package com.asb.analytics.domain.mercurius;

import org.bson.Document;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FairOdd {

    private Integer eventId;
    private String startDate;
    private boolean predictionAvailable;
    private String runnerName;
    private Float backFairPrice;

    public FairOdd(Integer eventId, String startDate, boolean predictionAvailable, String runnerName, Float backFairPrice) {
        this.eventId = eventId;
        this.startDate = startDate;
        this.predictionAvailable = predictionAvailable;
        this.runnerName = runnerName;
        this.backFairPrice = backFairPrice;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public boolean isPredictionAvailable() {
        return predictionAvailable;
    }

    public void setPredictionAvailable(boolean predictionAvailable) {
        this.predictionAvailable = predictionAvailable;
    }

    public String getRunnerName() {
        return runnerName;
    }

    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }

    public Float getBackFairPrice() {
        return backFairPrice;
    }

    public void setBackFairPrice(Float backFairPrice) {
        this.backFairPrice = backFairPrice;
    }

    public Document toDocument() {

        Map<String, Object> map = new HashMap<>();
        for (Field field : getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try { map.put(field.getName(), field.get(this)); } catch (Exception ignored) { }
        }

        return new Document(map);
    }
}
