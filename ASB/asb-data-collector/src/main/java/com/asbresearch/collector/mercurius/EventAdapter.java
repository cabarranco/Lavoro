package com.asbresearch.collector.mercurius;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapt all the event responses from the API
 *
 * @author Claudio Paolicelli
 */
public class EventAdapter {

    public static List<Prediction> getPredictionsTable(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<List<Prediction>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
