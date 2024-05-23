package com.asbresearch.betfair.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class InPlayResponse {
    private final int eventId;
    private final int eventTypeId;
    private final Score score;
    private final int timeElapsed;
    private final int elapsedRegularTime;
    private final List<UpdateDetail> updateDetails;
    private final String status;
    private final String inPlayMatchStatus;


    @JsonCreator(mode = PROPERTIES)
    public InPlayResponse(@JsonProperty("eventId") int eventId,
                          @JsonProperty("eventTypeId") int eventTypeId,
                          @JsonProperty("score") Score score,
                          @JsonProperty("timeElapsed") int timeElapsed,
                          @JsonProperty("elapsedRegularTime") int elapsedRegularTime,
                          @JsonProperty("updateDetails") List<UpdateDetail> updateDetails,
                          @JsonProperty("status") String status,
                          @JsonProperty("inPlayMatchStatus") String inPlayMatchStatus) {
        this.eventId = eventId;
        this.eventTypeId = eventTypeId;
        this.score = score;
        this.timeElapsed = timeElapsed;
        this.elapsedRegularTime = elapsedRegularTime;
        this.updateDetails = updateDetails;
        this.inPlayMatchStatus = inPlayMatchStatus;
        this.status = status;
    }
}
