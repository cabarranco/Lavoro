package com.asbresearch.betfair.ref;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonResponse<T> {
    private final String jsonrpc;
    private final T result;
    private final Object id;
    private final Boolean hasError;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonResponse(@JsonProperty("jsonrpc") String jsonrpc,
                        @JsonProperty("result") T result,
                        @JsonProperty("id") Object id,
                        @JsonProperty("hasError") Boolean hasError) {
        this.jsonrpc = jsonrpc;
        this.result = result;
        this.id = id;
        this.hasError = hasError;
    }
}
