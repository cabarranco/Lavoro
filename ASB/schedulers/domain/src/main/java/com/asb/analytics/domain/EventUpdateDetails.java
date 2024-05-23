package com.asb.analytics.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventUpdateDetails {

    private Date updateTime;
    private Integer updateId;
    private Integer matchTime;
    private Integer elapsedRegularTime;
    private String type;
    private String updateType;
    private String team;
    private String teamName;

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Integer updateId) {
        this.updateId = updateId;
    }

    public Integer getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Integer matchTime) {
        this.matchTime = matchTime;
    }

    public Integer getElapsedRegularTime() {
        return elapsedRegularTime;
    }

    public void setElapsedRegularTime(Integer elapsedRegularTime) {
        this.elapsedRegularTime = elapsedRegularTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getTeam() {
        return team == null ? "" : team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getTeamName() {
        return teamName == null ? "" : teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
