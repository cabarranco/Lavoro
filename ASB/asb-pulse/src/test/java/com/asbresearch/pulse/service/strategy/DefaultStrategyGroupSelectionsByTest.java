package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.RunnerCatalog;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.asbresearch.pulse.model.StrategySpecTest;
import com.asbresearch.pulse.service.BetfairEventService;
import com.asbresearch.pulse.service.MarketSelection;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultStrategyGroupSelectionsByTest {
    private static final Event EVENT = Event.of("29676731", "Maccabi Tel Aviv v Hapoel Raanana", "IL", "GMT", null);
    private static final RunnerCatalog HOME_RUNNER = RunnerCatalog.of(197970L, "Maccabi Tel Aviv");
    private static final MarketSelection MATCH_ODDS_HOME = MarketSelection.of(EVENT, "1.168084153", HOME_RUNNER, "MATCH ODDS", null);
    private static final MarketSelection CORRECT_SCORE_11 = MarketSelection.of(EVENT, "1.168084162", RunnerCatalog.of(3L, "1 - 1"), "CORRECT SCORE", null);
    private static final MarketSelection CORRECT_SCORE_00 = MarketSelection.of(EVENT, "1.168084162", RunnerCatalog.of(1L, "0 - 0"), "CORRECT SCORE", null);
    private static final MarketSelection CORRECT_SCORE_01 = MarketSelection.of(EVENT, "1.168084162", RunnerCatalog.of(4L, "0 - 1"), "CORRECT SCORE", null);

    private DefaultStrategy strategy;

    @BeforeEach
    void setUp() throws IOException {
        BetfairEventService betfairEventService = Mockito.mock(BetfairEventService.class);
        BetfairInPlayService betfairInPlayService = Mockito.mock(BetfairInPlayService.class);
        StrategyEventIgnoreContainer strategyEventIgnoreContainer = Mockito.mock(StrategyEventIgnoreContainer.class);
        strategy = new DefaultStrategy(StrategySpecTest.readStrategyFromResource(), betfairEventService, new BetfairMarketTypeMapping(), betfairInPlayService, strategyEventIgnoreContainer);
    }

    @Test
    void groupSelectionsByEvent_emptySeclections() {
        Map<Event, Set<MarketSelection>> groupSelectionsBy = strategy.groupSelectionsBy(emptySet(), MarketSelection::getEvent);
        assertThat(groupSelectionsBy, notNullValue());
        assertThat(groupSelectionsBy.size(), is(0));
    }

    @Test
    void groupSelectionByEvent_singleSelection() {
        Map<Event, Set<MarketSelection>> groupSelectionsBy = strategy.groupSelectionsBy(singleton(MATCH_ODDS_HOME), MarketSelection::getEvent);
        assertThat(groupSelectionsBy, notNullValue());
        assertThat(groupSelectionsBy.size(), is(1));
        assertThat(groupSelectionsBy.keySet(), hasItem(EVENT));
        assertThat(groupSelectionsBy.get(EVENT).size(), is(1));
        assertThat(groupSelectionsBy.get(EVENT), hasItem(MATCH_ODDS_HOME));
    }

    @Test
    void groupSelectionByEvent() {
        Map<Event, Set<MarketSelection>> groupSelectionsBy = strategy.groupSelectionsBy(
                Sets.newHashSet(MATCH_ODDS_HOME, CORRECT_SCORE_00, CORRECT_SCORE_01, CORRECT_SCORE_11),
                MarketSelection::getEvent);
        assertThat(groupSelectionsBy, notNullValue());
        assertThat(groupSelectionsBy.size(), is(1));
        assertThat(groupSelectionsBy.keySet(), hasItem(EVENT));
        assertThat(groupSelectionsBy.get(EVENT).size(), is(4));
        assertThat(groupSelectionsBy.get(EVENT), hasItem(MATCH_ODDS_HOME));
        assertThat(groupSelectionsBy.get(EVENT), hasItem(CORRECT_SCORE_00));
        assertThat(groupSelectionsBy.get(EVENT), hasItem(CORRECT_SCORE_01));
        assertThat(groupSelectionsBy.get(EVENT), hasItem(CORRECT_SCORE_11));
    }

}