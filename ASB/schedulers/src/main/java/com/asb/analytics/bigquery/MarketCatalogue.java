package com.asb.analytics.bigquery;

import java.util.HashMap;
import java.util.Map;

public class MarketCatalogue {

    private String marketId;
    private String marketName;
    private long selectionId;
    private String runnerName;
    private int eventId;
    private String eventName;
    private String startDate;
    private String competition;
    private Integer selection;
    private String frequency;

    public MarketCatalogue(
            String marketId,
            String marketName,
            long selectionId,
            String runnerName,
            int eventId,
            String eventName,
            String startDate,
            String competition,
            Integer selection,
            String frequency
    ) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.selectionId = selectionId;
        this.runnerName = runnerName;
        this.eventId = eventId;
        this.eventName = eventName;
        this.startDate = startDate;
        this.competition = competition;
        this.selection = selection;
        this.frequency = frequency;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("marketId", this.marketId);
        map.put("marketName", this.marketName);
        map.put("selectionId", this.selectionId);
        map.put("runnerName", this.runnerName);
        map.put("eventId", this.eventId);
        map.put("eventName", this.eventName);
        map.put("startDate", this.startDate);
        map.put("competition", this.competition);
        map.put("selection", this.selection);
        map.put("frequency", this.frequency);

        return map;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public long getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(long selectionId) {
        this.selectionId = selectionId;
    }

    public String getRunnerName() {
        return runnerName;
    }

    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getCompetition() {
        return competition;
    }

    public void setCompetition(String competition) {
        this.competition = competition;
    }

    public Integer getSelection() {
        return selection;
    }

    public void setSelection(Integer selection) {
        this.selection = selection;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}
