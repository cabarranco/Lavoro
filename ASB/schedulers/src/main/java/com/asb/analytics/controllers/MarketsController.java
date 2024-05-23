package com.asb.analytics.controllers;

import com.asb.analytics.api.betfair.betting.BetfairBetting;
import com.asb.analytics.domain.betfair.EventResponse;
import com.asb.analytics.domain.betfair.MarketBook;
import com.asb.analytics.domain.betfair.MarketCatalogue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.asb.analytics.domain.betfair.BetfairFields.*;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class MarketsController {

    private final String token;

    public MarketsController(String token) {
        this.token = token;
    }

    public List<MarketCatalogue> getMarketCatalogues(EventResponse eventResponse ) throws Exception {

        HashMap<String, Object> filtersMapCatalogues = new HashMap<>();

        filtersMapCatalogues.put("eventIds", Collections.singletonList(eventResponse.getEvent().getId()));
        filtersMapCatalogues.put("marketTypeCodes", new String[]{
                "MATCH_ODDS",
                "OVER_UNDER_25",
                "CORRECT_SCORE",
                "OVER_UNDER_05",
                "OVER_UNDER_15",
                "OVER_UNDER_35",
                "ASIAN_HANDICAP",
        });

        return BetfairBetting.init(token)
                .getMarketCatalogue(filtersMapCatalogues);
    }

    public List<MarketCatalogue> getMatchOddsMarketCatalogues(Integer eventId ) throws Exception {

        HashMap<String, Object> filtersMapCatalogues = new HashMap<>();

        filtersMapCatalogues.put("eventIds", Collections.singletonList(eventId));
        filtersMapCatalogues.put("marketTypeCodes", new String[]{"MATCH_ODDS"});

        return BetfairBetting.init(token)
                .getMarketCatalogue(filtersMapCatalogues);
    }

    public List<MarketBook> getMarketBooks(List<String> marketIds) throws Exception {

        HashMap<String, Object> filtersMapMarketBook = new HashMap<>();

        filtersMapMarketBook.put(MARKET_IDS, marketIds);
        filtersMapMarketBook.put(ORDER_PROJECTION, "EXECUTABLE");
        filtersMapMarketBook.put(MATCH_PROJECTION, "ROLLED_UP_BY_PRICE");

        HashMap<String, Object> priceProjectionFilters = new HashMap<>();
        priceProjectionFilters.put(PRICE_DATA, Collections.singletonList("EX_BEST_OFFERS"));
        filtersMapMarketBook.put(PRICE_PROJECTION, priceProjectionFilters);

        // GET MARKET BOOKS

        return BetfairBetting.init(token)
                .getMarketBook(filtersMapMarketBook);
    }
}
