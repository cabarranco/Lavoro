package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunnerProfitAndLoss {
    private final Long selectionId;
    private final Double ifWin;
    private final Double ifLose;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunnerProfitAndLoss(@JsonProperty("selectionId") Long selectionId,
                               @JsonProperty("ifWin") Double ifWin,
                               @JsonProperty("ifLose") Double ifLose) {
        this.selectionId = selectionId;
        this.ifWin = ifWin;
        this.ifLose = ifLose;
    }
}
