package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.InstructionReportErrorCode;
import com.asbresearch.betfair.ref.enums.InstructionReportStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ReplaceInstructionReport {
    private final InstructionReportStatus status;
    private final InstructionReportErrorCode errorCode;
    private final CancelInstructionReport cancelInstructionReport;
    private final PlaceInstructionReport placeInstructionReport;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReplaceInstructionReport(@JsonProperty("status") InstructionReportStatus status,
                                    @JsonProperty("errorCode") InstructionReportErrorCode errorCode,
                                    @JsonProperty("cancelInstructionReport") CancelInstructionReport cancelInstructionReport,
                                    @JsonProperty("placeInstructionReport") PlaceInstructionReport placeInstructionReport) {
        this.status = status;
        this.errorCode = errorCode;
        this.cancelInstructionReport = cancelInstructionReport;
        this.placeInstructionReport = placeInstructionReport;
    }
}
