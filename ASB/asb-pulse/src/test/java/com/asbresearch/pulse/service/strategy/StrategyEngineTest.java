package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.pulse.config.BetfairClientsConfig;
import com.asbresearch.pulse.config.EsaProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.service.MarketSnaps;
import com.asbresearch.pulse.service.OpportunityQueue;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

public class StrategyEngineTest {
    private EsaProperties esaProperties;
    private StrategyEngine strategyEngine;

    @BeforeEach
    void setUp() {
        OpportunityQueue opportunityQueue = Mockito.mock(OpportunityQueue.class);
        StrategyProperties strategyProperties = Mockito.mock(StrategyProperties.class);
        StrategyCache strategyCache = Mockito.mock(StrategyCache.class);
        BetfairClientsConfig clientsConfig = Mockito.mock(BetfairClientsConfig.class);
        esaProperties = Mockito.mock(EsaProperties.class);
        StrategyEventIgnoreContainer strategyEventIgnoreContainer = Mockito.mock(StrategyEventIgnoreContainer.class);
        MarketSnaps marketSnaps = Mockito.mock(MarketSnaps.class);
        BetfairInPlayService betfairInPlayService = Mockito.mock(BetfairInPlayService.class);

        Mockito.when(esaProperties.getMaxConnections()).thenReturn(10);
        Mockito.when(strategyProperties.getThreads()).thenReturn(1);
        strategyEngine = new StrategyEngine(marketSnaps,
                strategyCache,
                opportunityQueue,
                strategyProperties,
                clientsConfig,
                esaProperties,
                strategyEventIgnoreContainer,
                betfairInPlayService);
    }

    @AfterEach
    void shutDown() {
        if (strategyEngine != null) {
            strategyEngine.shutDown();
        }
    }

    @Test
    void partition() {
        Mockito.when(esaProperties.getMaxConnections()).thenReturn(10);
        Set<String> randomIds = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            randomIds.add(UUID.randomUUID().toString());
        }
        Iterable<List<String>> partitions = strategyEngine.partition(randomIds);
        assertThat(partitions, notNullValue());
        assertThat(Iterables.size(partitions), lessThanOrEqualTo(esaProperties.getMaxConnections()));
    }
}