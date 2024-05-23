package com.asb.analytics.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {

    private Team home;

    private Team away;

    private Integer numberOfYellowCards;

    private Integer numberOfRedCards;

    private Integer numberOfCornersFirstHalf;

    private Integer numberOfCornersSecondHalf;

    private Integer bookingPoints;

    public Team getHome() {
        return home;
    }

    public void setHome(Team home) {
        this.home = home;
    }

    public Team getAway() {
        return away;
    }

    public void setAway(Team away) {
        this.away = away;
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

    public Integer getNumberOfCornersFirstHalf() {
        return numberOfCornersFirstHalf != null
                ? numberOfCornersFirstHalf : 0;
    }

    public void setNumberOfCornersFirstHalf(Integer numberOfCornersFirstHalf) {
        this.numberOfCornersFirstHalf = numberOfCornersFirstHalf;
    }

    public Integer getNumberOfCornersSecondHalf() {
        return numberOfCornersSecondHalf != null
                ? numberOfCornersSecondHalf : 0;
    }

    public void setNumberOfCornersSecondHalf(Integer numberOfCornersSecondHalf) {
        this.numberOfCornersSecondHalf = numberOfCornersSecondHalf;
    }

    public Integer getBookingPoints() {
        return bookingPoints;
    }

    public void setBookingPoints(Integer bookingPoints) {
        this.bookingPoints = bookingPoints;
    }
}
