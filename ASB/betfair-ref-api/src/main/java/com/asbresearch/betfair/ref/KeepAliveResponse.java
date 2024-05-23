package com.asbresearch.betfair.ref;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class KeepAliveResponse {
    private final String token;
    private final String product;
    private final String status;
    private final String error;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KeepAliveResponse(@JsonProperty("token") String token,
                             @JsonProperty("product") String product,
                             @JsonProperty("status") String status,
                             @JsonProperty("error") String error) {
        this.token = token;
        this.product = product;
        this.status = status;
        this.error = error;
    }

    public static KeepAliveResponse error(String message) {
        return new KeepAliveResponse(null, null, null, message);
    }
}
