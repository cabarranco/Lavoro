package com.asb.analytics.api.adapters;

import com.asb.analytics.domain.EventType;
import com.asb.analytics.domain.LiveScore;
import com.asb.analytics.domain.betfair.*;
import com.asb.analytics.domain.mercurius.Prediction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapt all the event responses from the API
 *
 * @author Claudio Paolicelli
 */
public class EventAdapter {

    private static EventType getEventType(JSONObject object) throws JSONException {

        EventType eventType = new EventType();

        JSONObject type = object.getJSONObject("eventType");

        eventType.setId(type.getInt("id"));
        eventType.setMarketCount(object.getInt("marketCount"));
        eventType.setName(type.getString("name"));

        return eventType;
    }

    public static List<EventType> getEventTypeList(String json) {

        List<EventType> eventTypeList = new ArrayList<>();

        try {
            JSONArray list = new JSONArray(json);

            for (int i = 0; i < list.length(); i++) {
                try {
                    eventTypeList.add(getEventType(list.getJSONObject(i)));
                } catch (JSONException ignore) {}
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return eventTypeList;
    }

    public static List<EventResponse> getEvents(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<List<EventResponse>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<MarketCatalogue> getMarketCatalogues(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<List<MarketCatalogue>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<MarketBook> getMarketBooks(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<List<MarketBook>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Prediction> getPredictionsTable(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<List<Prediction>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static List<LiveScore> getLiveScore(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<List<LiveScore>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static PlaceExecutionReport getBetResponse(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<PlaceExecutionReport>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getCompetitionIds(String json) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Competition> competitions = mapper.readValue(json, new TypeReference<List<Competition>>(){});

            return competitions.stream()
                    .map(
                            // map the element to a CompetitionResponse in present
                            competitionResponse -> competitionResponse.getCompetition().getId()
                    ).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }


    }

    public static Set<String> getEventIds(List<EventResponse> events) {

            return events.stream()
                    .map(
                            event -> event.getEvent().getId()
                    ).collect(Collectors.toSet());

    }

    public static List<String> getMarketIds(List<MarketCatalogue> events) {

        return events.stream()
                .map(MarketCatalogue::getMarketId).collect(Collectors.toList());

    }
}
