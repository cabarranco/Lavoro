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
public class TransferResponse {
    private final String transactionId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TransferResponse(@JsonProperty("transactionId") String transactionId) {
        this.transactionId = transactionId;
    }
}
