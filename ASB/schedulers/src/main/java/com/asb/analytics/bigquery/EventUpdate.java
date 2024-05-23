package com.asb.analytics.bigquery;

import org.bson.Document;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class EventUpdate {

    private Integer eventId;
    private String team;
    private String name;
    private Integer score;
    private Integer numberOfYellowCards;
    private Integer numberOfRedCards;
    private String updateTime;
    private Integer matchTime;
    private String updateType;
    private String inPlayMatchStatus;
    private Integer numberOfCorners;
    private String eventScore;
    private String serverTimestamp;

    public EventUpdate(
            Integer eventId,
            String team,
            String name,
            Integer score,
            Integer numberOfYellowCards,
            Integer numberOfRedCards,
            String updateTime,
            Integer matchTime,
            String updateType,
            String inPlayMatchStatus,
            Integer numberOfCorners,
            String eventScore,
            String serverTimestamp
    ) {
        this.eventId = eventId;
        this.team = team;
        this.name = name;
        this.score = score;
        this.numberOfYellowCards = numberOfYellowCards;
        this.numberOfRedCards = numberOfRedCards;
        this.updateTime = updateTime;
        this.matchTime = matchTime;
        this.updateType = updateType;
        this.inPlayMatchStatus = inPlayMatchStatus;
        this.numberOfCorners = numberOfCorners;
        this.eventScore = eventScore;
        this.serverTimestamp = serverTimestamp;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getNumberOfYellowCards() {
        return numberOfYellowCards;
    }

    public void setNumberOfYellowCards(Integer numberOfYellowCards) {
        this.numberOfYellowCards = numberOfYellowCards;
    }

    public Integer getNumberOfRedCards() {
        return numberOfRedCards;
    }

    public void setNumberOfRedCards(Integer numberOfRedCards) {
        this.numberOfRedCards = numberOfRedCards;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Integer matchTime) {
        this.matchTime = matchTime;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getInPlayMatchStatus() {
        return inPlayMatchStatus;
    }

    public void setInPlayMatchStatus(String inPlayMatchStatus) {
        this.inPlayMatchStatus = inPlayMatchStatus;
    }

    public Integer getNumberOfCorners() {
        return numberOfCorners;
    }

    public void setNumberOfCorners(Integer numberOfCorners) {
        this.numberOfCorners = numberOfCorners;
    }

    public String getEventScore() {
        return eventScore;
    }

    public void setEventScore(String eventScore) {
        this.eventScore = eventScore;
    }

    public String getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(String serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
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
