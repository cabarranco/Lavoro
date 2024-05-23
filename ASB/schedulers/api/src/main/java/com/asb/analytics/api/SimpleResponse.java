package com.asb.analytics.api;

import java.util.List;
import java.util.Map;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class SimpleResponse {

    private int code;
    private String message;
    private Map<String, List<String>> headers;

    private String body;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
