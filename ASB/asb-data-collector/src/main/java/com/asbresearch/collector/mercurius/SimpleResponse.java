package com.asbresearch.collector.mercurius;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@Data
public class SimpleResponse {

    private int code;
    private String message;
    private Map<String, List<String>> headers;
    private String body;
}
