package com.asbresearch.betfair.ref;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonInclude(NON_NULL)
public class JsonRequest {
    public static final String JSON_RPC_VERSION = "2.0";

    private final String jsonrpc;
    private final String method;
    private final int id;
    private final Map<String, Object> params;


    public JsonRequest(String jsonrpc, String method, int id, Map<String, Object> params) {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.id = id;
        this.params = params;
    }

    public JsonRequest(String method, int id, Map<String, Object> params) {
        this(JSON_RPC_VERSION, method, id, params);
    }

    public static JsonRequest of(String method, Map<String, Object> params) {
        return new JsonRequest(method, 1, params);
    }
}
