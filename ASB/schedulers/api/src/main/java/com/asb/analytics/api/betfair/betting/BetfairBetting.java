package com.asb.analytics.api.betfair.betting;

import com.asb.analytics.api.Config;
import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.api.adapters.EventAdapter;
import com.asb.analytics.api.betfair.account.BetfairAccount;
import com.asb.analytics.domain.EventType;
import com.asb.analytics.domain.LiveScore;
import com.asb.analytics.domain.betfair.EventResponse;
import com.asb.analytics.domain.betfair.MarketBook;
import com.asb.analytics.domain.betfair.MarketCatalogue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Collection of all the betting api methods from Betfair
 *
 * @author Claudio Paolicelli
 */
public class BetfairBetting {

    private final String sessionToken;
    private final Gson gson;

    private BetfairBetting(String sessionToken) {
        this.sessionToken = sessionToken;
        this.gson = new GsonBuilder().create();
    }

    /**
     * Static constructor for betfair betting api class
     *
     * @param sessionToken get from {@link BetfairAccount}
     * @return new instance of {@link BetfairBetting}
     */
    public static BetfairBetting init(String sessionToken) {
        return new BetfairBetting(sessionToken);
    }

    /**
     * Get list of events
     *
     * @return String json of events list
     */
    public List<EventType> listEventsTypes() {

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listEventTypes/")
                    .timeout(5000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body("{\"filter\":{ }}")
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getEventTypeList(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<EventResponse> getListEvents(HashMap<String, Object> filtersMap) {
        String filters = gson.toJson(filtersMap);

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listEvents/")
                    .timeout(5000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body(String.format("{\"filter\": %s }", filters))
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getEvents(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getCompetitionIds(HashMap<String, Object> filtersMap) {

        String filters = gson.toJson(filtersMap);

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listCompetitions/")
                    .timeout(5000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body(String.format("{\"filter\": %s }", filters))
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getCompetitionIds(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<MarketCatalogue> getMarketCatalogue(HashMap<String, Object> filtersMap) {

        HashMap<String, Object> filters = new HashMap<>();

        filters.put("filter", filtersMap);
        filters.put("maxResults", 1000);

        filters.put("marketProjection", Arrays.asList("EVENT","RUNNER_DESCRIPTION","COMPETITION"));

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listMarketCatalogue/")
                    .timeout(50000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body(gson.toJson(filters))
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getMarketCatalogues(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<MarketBook> getMarketBook(HashMap<String, Object> filtersMap) {

        String filters = gson.toJson(filtersMap);

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listMarketBook/")
                    .timeout(60000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body(filters)
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getMarketBooks(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<LiveScore> getLiveScore(String... eventIds) {

        if (eventIds == null || eventIds.length == 0)
            return new ArrayList<>();

        String ids = eventIds[0];

        if (eventIds.length > 1) {
            ids = String.join(",", eventIds);
        }

        try {
            SimpleResponse response = HttpConnector
                    .connect(String.format("https://ips.betfair.com/inplayservice/v1/eventTimelines?_ak=nzIFcwyWhrlwYMrh&alt=json&eventIds=%s&locale=en", ids))
                    .timeout(5000)
                    .method(HttpConnector.GET)
                    .execute();

            return EventAdapter.getLiveScore(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
