package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.InstructionReportErrorCode;
import com.asbresearch.betfair.ref.enums.InstructionReportStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class UpdateExecutionReport {
    private final String customerRef;
    private final InstructionReportStatus status;
    private final InstructionReportErrorCode errorCode;
    private final List<UpdateInstructionReport> instructionReports;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UpdateExecutionReport(@JsonProperty("customerRef") String customerRef,
                                 @JsonProperty("status") InstructionReportStatus status,
                                 @JsonProperty("errorCode") InstructionReportErrorCode errorCode,
                                 @JsonProperty("instructionReports") List<UpdateInstructionReport> instructionReports) {
        this.customerRef = customerRef;
        this.status = status;
        this.errorCode = errorCode;
        this.instructionReports = instructionReports != null ? ImmutableList.copyOf(instructionReports) : ImmutableList.of();
    }
}
