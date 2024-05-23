package com.asbresearch.betfair.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
    private final String name;
    private final Integer score;
    private final Integer halfTimeScore;
    private final Integer fullTimeScore;
    private final Integer penaltiesScore;
    private final Integer numberOfYellowCards;
    private final Integer numberOfRedCards;
    private final Integer numberOfCornersFirstHalf;
    private final Integer numberOfCornersSecondHalf;
    private final Integer bookingPoints;

    @JsonCreator(mode = PROPERTIES)
    public Team(@JsonProperty("name") String name,
                @JsonProperty("score") Integer score,
                @JsonProperty("halfTimeScore") Integer halfTimeScore,
                @JsonProperty("fullTimeScore") Integer fullTimeScore,
                @JsonProperty("penaltiesScore") Integer penaltiesScore,
                @JsonProperty("numberOfYellowCards") Integer numberOfYellowCards,
                @JsonProperty("numberOfRedCards") Integer numberOfRedCards,
                @JsonProperty("numberOfCornersFirstHalf") Integer numberOfCornersFirstHalf,
                @JsonProperty("numberOfCornersSecondHalf") Integer numberOfCornersSecondHalf,
                @JsonProperty("bookingPoints") Integer bookingPoints) {
        this.name = name;
        this.score = score;
        this.halfTimeScore = halfTimeScore;
        this.fullTimeScore = fullTimeScore;
        this.penaltiesScore = penaltiesScore;
        this.numberOfYellowCards = numberOfYellowCards;
        this.numberOfRedCards = numberOfRedCards;
        this.numberOfCornersFirstHalf = numberOfCornersFirstHalf;
        this.numberOfCornersSecondHalf = numberOfCornersSecondHalf;
        this.bookingPoints = bookingPoints;
    }
}
