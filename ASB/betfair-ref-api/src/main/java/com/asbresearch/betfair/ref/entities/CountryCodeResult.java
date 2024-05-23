package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryCodeResult {
    private final String countryCode;
    private final int marketCount;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CountryCodeResult(@JsonProperty("countryCode") String countryCode,
                             @JsonProperty("marketCount") int marketCount) {
        this.countryCode = countryCode;
        this.marketCount = marketCount;
    }
}
