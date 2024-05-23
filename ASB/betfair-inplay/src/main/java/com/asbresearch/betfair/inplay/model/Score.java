package com.asbresearch.betfair.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {
    private final Team home;
    private final Team away;
    private final Integer numberOfYellowCards;
    private final Integer numberOfRedCards;
    private final Integer numberOfCornersFirstHalf;
    private final Integer numberOfCornersSecondHalf;
    private final Integer bookingPoints;

    @JsonCreator(mode = PROPERTIES)
    public Score(@JsonProperty("home") Team home,
                 @JsonProperty("away") Team away,
                 @JsonProperty("numberOfYellowCards") Integer numberOfYellowCards,
                 @JsonProperty("numberOfRedCards") Integer numberOfRedCards,
                 @JsonProperty("numberOfCornersFirstHalf") Integer numberOfCornersFirstHalf,
                 @JsonProperty("numberOfCornersSecondHalf") Integer numberOfCornersSecondHalf,
                 @JsonProperty("bookingPoints") Integer bookingPoints) {
        this.home = home;
        this.away = away;
        this.numberOfYellowCards = numberOfYellowCards;
        this.numberOfRedCards = numberOfRedCards;
        this.numberOfCornersFirstHalf = numberOfCornersFirstHalf;
        this.numberOfCornersSecondHalf = numberOfCornersSecondHalf;
        this.bookingPoints = bookingPoints;
    }
}
