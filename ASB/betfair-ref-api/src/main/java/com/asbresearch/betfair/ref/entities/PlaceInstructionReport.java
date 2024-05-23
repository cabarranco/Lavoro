package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.InstructionReportErrorCode;
import com.asbresearch.betfair.ref.enums.InstructionReportStatus;
import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class PlaceInstructionReport {
    private final InstructionReportStatus status;
    private final InstructionReportErrorCode errorCode;
    private final PlaceInstruction instruction;
    private final String betId;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant placedDate;
    private final double averagePriceMatched;
    private final double sizeMatched;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PlaceInstructionReport(@JsonProperty("status") InstructionReportStatus status,
                                  @JsonProperty("errorCode") InstructionReportErrorCode errorCode,
                                  @JsonProperty("instruction") PlaceInstruction instruction,
                                  @JsonProperty("betId") String betId,
                                  @JsonProperty("placedDate") Instant placedDate,
                                  @JsonProperty("averagePriceMatched") double averagePriceMatched,
                                  @JsonProperty("sizeMatched") double sizeMatched) {
        this.status = status;
        this.errorCode = errorCode;
        this.instruction = instruction;
        this.betId = betId;
        this.placedDate = placedDate;
        this.averagePriceMatched = averagePriceMatched;
        this.sizeMatched = sizeMatched;
    }
}
