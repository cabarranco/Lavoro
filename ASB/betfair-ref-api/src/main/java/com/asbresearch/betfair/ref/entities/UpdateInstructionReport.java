package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.InstructionReportErrorCode;
import com.asbresearch.betfair.ref.enums.InstructionReportStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class UpdateInstructionReport {
    private final InstructionReportStatus status;
    private final InstructionReportErrorCode errorCode;
    @EqualsAndHashCode.Include
    private final UpdateInstruction instruction;

    @JsonCreator(mode = PROPERTIES)
    public UpdateInstructionReport(@JsonProperty("status") InstructionReportStatus status,
                                   @JsonProperty("errorCode") InstructionReportErrorCode errorCode,
                                   @JsonProperty("instruction") UpdateInstruction instruction) {
        this.status = status;
        this.errorCode = errorCode;
        this.instruction = instruction;
    }
}
