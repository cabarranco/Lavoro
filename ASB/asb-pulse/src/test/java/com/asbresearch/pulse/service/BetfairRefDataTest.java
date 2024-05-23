package com.asbresearch.pulse.service;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.CompetitionResult;
import com.asbresearch.betfair.ref.entities.CountryCodeResult;
import com.asbresearch.betfair.ref.entities.EventResult;
import com.asbresearch.betfair.ref.entities.EventTypeResult;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.MarketFilter;
import com.asbresearch.betfair.ref.entities.MarketTypeResult;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import com.asbresearch.betfair.ref.util.Helpers;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class BetfairRefDataTest {
    private BetfairReferenceClient client = new BetfairReferenceClient(Exchange.UK, "ZBsLSgTAiftsAy2R");
    private boolean loggedIn;

    @BeforeEach
    void login() throws LoginException {
        loggedIn = client.login("fdr@asbresearch.com", "asbcheqai87");
    }

    @Test
    void listCountries() {
        if (loggedIn) {
            BetfairServerResponse<List<CountryCodeResult>> response = client.listCountries(new MarketFilter());
            if (response != null) {
                response.getResponse().forEach(countryCodeResult -> System.out.println(countryCodeResult.getCountryCode()));
            }
        }
    }


    @Test
    void listEventTypes() {
        if (loggedIn) {
            BetfairServerResponse<List<EventTypeResult>> response = client.listEventTypes(new MarketFilter());
            if (response != null) {
                List<EventTypeResult> eventTypeResults = response.getResponse();
                eventTypeResults.forEach(eventType -> System.out.println(eventType.getEventType()));
            }
        }
    }

    @Test
    void listMarketTypes() {
        if (loggedIn) {
            BetfairServerResponse<List<MarketTypeResult>> response = client.listMarketTypes(new MarketFilter());
            if (response != null) {
                List<MarketTypeResult> marketTypeResults = response.getResponse();
                marketTypeResults.forEach(marketType -> System.out.println(marketType.getMarketType()));
            }
        }
    }

    @Test
    void listCompetitions() {
        if(loggedIn) {
            MarketFilter marketFilter = new MarketFilter();
            marketFilter.setEventTypeIds(Collections.singleton("1"));
            BetfairServerResponse<List<CompetitionResult>> response = client.listCompetitions(marketFilter);
            if (response != null) {
                List<CompetitionResult> competitionResults = response.getResponse();
                competitionResults.forEach(competitionResult -> System.out.println(competitionResult.getCompetition()));
            }
        }
    }

    @Test
    void listEvents() {
        if(loggedIn) {
            Instant now = Instant.now();
            TimeRange localTimeRange = new TimeRange(now, now.plus(1, ChronoUnit.DAYS));
            MarketFilter filter = new MarketFilter();
            filter.setEventTypeIds(Collections.singleton("1"));
            filter.setMarketStartTime(localTimeRange);
            BetfairServerResponse<List<EventResult>> response = client.listEvents(filter);
            if (response != null) {
                response.getResponse().forEach(eventResult -> System.out.println(eventResult.getEvent()));
            }
        }
    }

    @Test
    void listMarketCatalogue() {
        if (loggedIn) {
            MarketFilter filter = new MarketFilter();
            filter.setEventIds(Collections.singleton("29662267"));
            filter.setMarketTypeCodes(Sets.newHashSet("MATCH_ODDS", "CORRECT_SCORE"));
            BetfairServerResponse<List<MarketCatalogue>> response = client.listMarketCatalogue(filter, Helpers.soccerMatchProjection(), MarketSort.FIRST_TO_START, 40);
            if (response != null) {
                response.getResponse().forEach(System.out::println);
            }
        }
    }
}
