package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AccountDetailsResponse {
    private final String currencyCode;
    private final String firstName;
    private final String lastName;
    private final String localeCode;
    private final String region;
    private final String timeZone;
    private final double discountRate;
    private final int pointsBalance;
    private final String countryCode;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AccountDetailsResponse(@JsonProperty("currencyCode") String currencyCode,
                                  @JsonProperty("firstName") String firstName,
                                  @JsonProperty("lastName") String lastName,
                                  @JsonProperty("localeCode") String localeCode,
                                  @JsonProperty("region") String region,
                                  @JsonProperty("timeZone") String timeZone,
                                  @JsonProperty("discountRate") double discountRate,
                                  @JsonProperty("pointsBalance") int pointsBalance,
                                  @JsonProperty("countryCode") String countryCode) {
        this.currencyCode = currencyCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.localeCode = localeCode;
        this.region = region;
        this.timeZone = timeZone;
        this.discountRate = discountRate;
        this.pointsBalance = pointsBalance;
        this.countryCode = countryCode;
    }
}
