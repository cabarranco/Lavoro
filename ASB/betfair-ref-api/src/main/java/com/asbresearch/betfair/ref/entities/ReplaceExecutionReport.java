package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.ExecutionReportErrorCode;
import com.asbresearch.betfair.ref.enums.ExecutionReportStatus;
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
public class ReplaceExecutionReport {
    private final String customerRef;
    private final ExecutionReportStatus status;
    private final ExecutionReportErrorCode errorCode;
    private final String marketId;
    private final List<ReplaceInstructionReport> instructionReports;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReplaceExecutionReport(@JsonProperty("customerRef") String customerRef,
                                  @JsonProperty("status") ExecutionReportStatus status,
                                  @JsonProperty("errorCode") ExecutionReportErrorCode errorCode,
                                  @JsonProperty("marketId") String marketId,
                                  @JsonProperty("instructionReports") List<ReplaceInstructionReport> instructionReports) {
        this.customerRef = customerRef;
        this.status = status;
        this.errorCode = errorCode;
        this.marketId = marketId;
        this.instructionReports = instructionReports != null ? ImmutableList.copyOf(instructionReports) : ImmutableList.of();
    }
}
