package com.asbresearch.pulse.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpportunityBetsResult {
    private final int totalRows;
    private final List<OpportunityBet> opportunityBets;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OpportunityBetsResult(@JsonProperty("totalRows") int totalRows,
                                 @JsonProperty("opportunityBets") List<OpportunityBet> opportunityBets) {
        this.totalRows = totalRows;
        this.opportunityBets = opportunityBets;
    }
}
