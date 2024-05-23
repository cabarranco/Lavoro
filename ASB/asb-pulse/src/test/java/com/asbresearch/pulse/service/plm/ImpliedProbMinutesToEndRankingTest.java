package com.asbresearch.pulse.service.plm;

import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import com.asbresearch.pulse.service.MarketSelection;
import com.asbresearch.pulse.service.SelectionPrice;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;


import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class ImpliedProbMinutesToEndRankingTest {

    @Test
    void top() {
        Instant currentTime = Instant.parse("2020-05-07T20:30:00Z");
        Event event1 = new Event("id1", "name1", "CC1", "tz", Instant.parse("2020-05-07T19:00:00Z"));
        MarketSelection marketSelection = MarketSelection.of(event1, "marketId", null, "ODD", new UserRunnerCode("MO.H.B"));
        OpportunityBet bet1 = new OpportunityBet(event1, "STRAT", currentTime,
                Collections.singletonList(OpportunitySelection.of(marketSelection, SelectionPrice.back(new PriceSize(1.43, 10.0)))), "allocatorId", null, true);

        Event event2 = new Event("id1", "name1", "CC1", "tz", Instant.parse("2020-05-07T22:45:00Z"));
        OpportunityBet bet2 = new OpportunityBet(event2, "STRAT", currentTime,
                Collections.singletonList(OpportunitySelection.of(marketSelection, SelectionPrice.back(new PriceSize(1.25, 10.0)))), "allocatorId", null, true);

        Event event3 = new Event("id1", "name1", "CC1", "tz", Instant.parse("2020-05-07T20:45:00Z"));
        OpportunityBet bet3 = new OpportunityBet(event3, "STRAT", currentTime,
                Collections.singletonList(OpportunitySelection.of(marketSelection, SelectionPrice.back(new PriceSize(2.0, 10.0)))), "allocatorId", null, true);

        Event event4 = new Event("id1", "name1", "CC1", "tz", Instant.parse("2020-05-07T20:50:00Z"));
        OpportunityBet bet4 = new OpportunityBet(event4, "STRAT", currentTime,
                Collections.singletonList(OpportunitySelection.of(marketSelection, SelectionPrice.back(new PriceSize(1.54, 10.0)))), "allocatorId", null, true);

        ImpliedProbMinutesToEndRanking ranking = new ImpliedProbMinutesToEndRanking(currentTime, Arrays.asList(bet2, bet1, bet4, bet3));

        OpportunityBet top = ranking.rank().get(0);
        assertThat(top, notNullValue());
        assertThat(top, CoreMatchers.sameInstance(bet1));
    }
}