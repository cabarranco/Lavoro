package com.asb.analytics.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveScore {

    private Integer eventId;

    private Integer eventTypeId;

    private Score score;

    private Integer timeElapsed;

    private Integer elapsedRegularTime;

    private List<EventUpdateDetails> updateDetails = new ArrayList<>();

    private String status;

    private String inPlayMatchStatus;

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(Integer eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    public Integer getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(Integer timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    public Integer getElapsedRegularTime() {
        return elapsedRegularTime;
    }

    public void setElapsedRegularTime(Integer elapsedRegularTime) {
        this.elapsedRegularTime = elapsedRegularTime;
    }

    public List<EventUpdateDetails> getUpdateDetails() {
        return updateDetails;
    }

    public void setUpdateDetails(List<EventUpdateDetails> updateDetails) {
        this.updateDetails = updateDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInPlayMatchStatus() {

        if ("KickOff".equalsIgnoreCase(inPlayMatchStatus))
            return "FirstHalf";

        if ("SecondHalfKickOff".equalsIgnoreCase(inPlayMatchStatus))
            return "SecondHalf";

        return inPlayMatchStatus;
    }

    public void setInPlayMatchStatus(String inPlayMatchStatus) {
        this.inPlayMatchStatus = inPlayMatchStatus;
    }
}
