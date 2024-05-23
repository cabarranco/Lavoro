package com.asbresearch.pulse.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

import static com.google.common.base.Preconditions.checkArgument;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpportunityBetsRequest {
    private final List<String> strategyIds;
    private final String date;
    private final int index;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OpportunityBetsRequest(@JsonProperty("strategyIds") List<String> strategyIds,
                                  @JsonProperty("date") String date,
                                  @JsonProperty("index") int index) {

        checkArgument(index > 0, "index must be greater than zero");

        this.strategyIds = strategyIds;
        this.date = date;
        this.index = index;
    }
}
