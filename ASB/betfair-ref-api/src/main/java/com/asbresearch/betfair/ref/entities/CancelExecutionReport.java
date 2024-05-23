package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.ExecutionReportErrorCode;
import com.asbresearch.betfair.ref.enums.ExecutionReportStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class CancelExecutionReport {
    private String customerRef;
    private ExecutionReportStatus status;
    private ExecutionReportErrorCode errorCode;
    private String marketId;
    private List<CancelInstructionReport> instructionReports;
}
