package com.asbresearch.pulse.service;


import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class OpportunityQueueTest {
    private OpportunityQueue opportunityQueue;

    @BeforeEach
    void setUp() {
        opportunityQueue = new OpportunityQueue(new AppProperties());
    }

    @Test
    void availableOpportunities() {
        Instant now = Instant.ofEpochMilli(1588749530422L);
        Event event1 = Event.of("1", "", "", "");
        Event event2 = Event.of("2", "", "", "");
        String strat1 = "STRAT1";
        String strat2 = "STRAT2";
        List<OpportunitySelection> selections = Collections.emptyList();
        opportunityQueue.add(new OpportunityBet(event1, strat1, now, selections, "allocatorId", null, true));
        opportunityQueue.add(new OpportunityBet(event1, strat1, now.plusSeconds(2), selections, "allocatorId", null, true));
        OpportunityBet opportunityBet1 = new OpportunityBet(event1, strat1, now.plusSeconds(4), selections, "allocatorId", null, true);
        opportunityQueue.add(opportunityBet1);
        opportunityQueue.add(new OpportunityBet(event1, strat2, now, selections, "allocatorId", null, true));
        opportunityQueue.add(new OpportunityBet(event1, strat2, now.plusSeconds(2), selections, "allocatorId", null, true));
        OpportunityBet opportunityBet2 = new OpportunityBet(event1, strat2, now.plusSeconds(4), selections, "allocatorId", null, true);
        opportunityQueue.add(opportunityBet2);
        opportunityQueue.add(new OpportunityBet(event2, strat1, now, selections, "allocatorId", null, true));
        opportunityQueue.add(new OpportunityBet(event2, strat1, now.plusSeconds(2), selections, "allocatorId", null, true));
        OpportunityBet opportunityBet3 = new OpportunityBet(event2, strat1, now.plusSeconds(4), selections, "allocatorId", null, true);
        opportunityQueue.add(opportunityBet3);
        opportunityQueue.add(new OpportunityBet(event2, strat2, now, selections, "allocatorId", null, true));
        opportunityQueue.add(new OpportunityBet(event2, strat2, now.plusSeconds(2), selections, "allocatorId", null, true));
        OpportunityBet opportunityBet4 = new OpportunityBet(event2, strat2, now.plusSeconds(4), selections, "allocatorId", null, true);
        opportunityQueue.add(opportunityBet4);

        List<OpportunityBet> opportunityBets = opportunityQueue.availableOpportunities(Collections.emptyList());
        assertThat(opportunityBets, notNullValue());
        assertThat(opportunityBets.size(), is(4));
        assertThat(opportunityBets, hasItem(opportunityBet1));
        assertThat(opportunityBets, hasItem(opportunityBet2));
        assertThat(opportunityBets, hasItem(opportunityBet3));
        assertThat(opportunityBets, hasItem(opportunityBet4));
    }

    @Test
    void availableOpportunities_epmty() {
        List<OpportunityBet> opportunityBets = opportunityQueue.availableOpportunities(Collections.emptyList());
        assertThat(opportunityBets, notNullValue());
        assertThat(opportunityBets.size(), is(0));
    }
}