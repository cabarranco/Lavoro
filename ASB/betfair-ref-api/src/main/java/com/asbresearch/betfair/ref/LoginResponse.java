package com.asbresearch.betfair.ref;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private final String token;
    private final String status;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LoginResponse(@JsonProperty("token") String token, @JsonProperty("status") String status) {
        this.token = token;
        this.status = status;
    }
}
