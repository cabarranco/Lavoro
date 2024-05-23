package com.asb.analytics.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

    private String name;

    private Integer score;

    private Integer halfTimeScore;

    private Integer fullTimeScore;

    private Integer penaltiesScore;

    private Integer numberOfYellowCards;

    private Integer numberOfRedCards;

    private Integer numberOfCornersFirstHalf;

    private Integer numberOfCornersSecondHalf;

    private Integer bookingPoints;

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

    public Integer getHalfTimeScore() {
        return halfTimeScore;
    }

    public void setHalfTimeScore(Integer halfTimeScore) {
        this.halfTimeScore = halfTimeScore;
    }

    public Integer getFullTimeScore() {
        return fullTimeScore;
    }

    public void setFullTimeScore(Integer fullTimeScore) {
        this.fullTimeScore = fullTimeScore;
    }

    public Integer getPenaltiesScore() {
        return penaltiesScore;
    }

    public void setPenaltiesScore(Integer penaltiesScore) {
        this.penaltiesScore = penaltiesScore;
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
        return numberOfCornersFirstHalf;
    }

    public void setNumberOfCornersFirstHalf(Integer numberOfCornersFirstHalf) {
        this.numberOfCornersFirstHalf = numberOfCornersFirstHalf;
    }

    public Integer getNumberOfCornersSecondHalf() {
        return numberOfCornersSecondHalf;
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
