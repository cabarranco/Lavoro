package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Betfair response for placeOrder
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties
public class PlaceExecutionReport {

    private String customerRef;

    private String status;

    private String errorCode;

    private String marketId;

    private List<PlaceInstructionReport> instructionReports;

    // GETTERS & SETTERS

    public String getCustomerRef() {
        return customerRef;
    }

    public void setCustomerRef(String customerRef) {
        this.customerRef = customerRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public List<PlaceInstructionReport> getInstructionReports() {
        return instructionReports;
    }

    public void setInstructionReports(List<PlaceInstructionReport> instructionReports) {
        this.instructionReports = instructionReports;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public PlaceInstructionReport getPlaceInstructionReport() {
        return this.instructionReports.get(0);
    }
}
