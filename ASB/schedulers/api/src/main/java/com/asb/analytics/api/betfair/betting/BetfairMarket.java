package com.asb.analytics.api.betfair.betting;

import com.asb.analytics.api.Config;
import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.api.adapters.EventAdapter;
import com.asb.analytics.api.betfair.account.BetfairAccount;
import com.asb.analytics.domain.EventType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of all the betting api methods from Betfair
 *
 * @author Claudio Paolicelli
 */
public class BetfairMarket {

    private final String sessionToken;

    private BetfairMarket(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /**
     * Static constructor for betfair betting api class
     *
     * @param sessionToken get from {@link BetfairAccount}
     * @return new instance of {@link BetfairMarket}
     */
    public static BetfairMarket init(String sessionToken) {
        return new BetfairMarket(sessionToken);
    }

    /**
     * Get list of market types filtering by event type SOCCER
     *
     * @return String json of events list
     */
    public List<EventType> listMarketTypes(int eventTypeId) {

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_BETTING_ENDPOINT + "listMarketTypes/")
                    .timeout(5000)
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("X-Authentication", this.sessionToken)
                    .header("content-type", "application/json")
                    .body(String.format("{\"filter\":{ \"eventTypeIds\" : [%d] }}", eventTypeId))
                    .method(HttpConnector.POST)
                    .execute();

            return EventAdapter.getEventTypeList(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
