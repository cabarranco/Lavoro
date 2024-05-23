package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class CurrencyRate {
    @EqualsAndHashCode.Include
    private final String currencyCode;
    private final double rate;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CurrencyRate(@JsonProperty("currencyCode") String currencyCode, @JsonProperty("rate") double rate) {
        this.currencyCode = currencyCode;
        this.rate = rate;
    }
}

