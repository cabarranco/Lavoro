package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountFundsResponse {
    private final double availableToBetBalance;
    private final double exposure;
    private final double retainedCommission;
    private final double exposureLimit;
    private final double discountRate;
    private final double pointsBalance;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AccountFundsResponse(@JsonProperty("availableToBetBalance") double availableToBetBalance,
                                @JsonProperty("exposure") double exposure,
                                @JsonProperty("retainedCommission") double retainedCommission,
                                @JsonProperty("exposureLimit") double exposureLimit,
                                @JsonProperty("discountRate") double discountRate,
                                @JsonProperty("pointsBalance") double pointsBalance) {
        this.availableToBetBalance = availableToBetBalance;
        this.exposure = exposure;
        this.retainedCommission = retainedCommission;
        this.exposureLimit = exposureLimit;
        this.discountRate = discountRate;
        this.pointsBalance = pointsBalance;
    }
}
