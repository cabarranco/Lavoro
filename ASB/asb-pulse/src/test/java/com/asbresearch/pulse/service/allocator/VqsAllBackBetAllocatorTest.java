package com.asbresearch.pulse.service.allocator;

import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import com.asbresearch.pulse.service.MarketSelection;
import com.asbresearch.pulse.service.SelectionPrice;
import com.asbresearch.pulse.service.plm.AccountAllocations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VqsAllBackBetAllocatorTest {
    private Event event;
    private MarketSelection a0;
    private MarketSelection a1;
    private MarketSelection a2;
    private MarketSelection a3;
    private VqsAllBackBetAllocator allocator;

    @BeforeEach
    void setUp() {
        event = new Event("id", "name", "countryCode", "timeZone", Instant.now());
        a0 = MarketSelection.of(event, null, null, null, new UserRunnerCode("MO.H.B"));
        a1 = MarketSelection.of(event, null, null, null, new UserRunnerCode("CS.00.B"));
        a2 = MarketSelection.of(event, null, null, null, new UserRunnerCode("CS.01.B"));
        a3 = MarketSelection.of(event, null, null, null, new UserRunnerCode("CS.11.B"));
        AccountAllocations accountAllocations = Mockito.mock(AccountAllocations.class);
        Mockito.when(accountAllocations.getOpportunityMaxAllocationSum()).thenReturn(67.0);
        AccountProperties accountProperties = Mockito.mock(AccountProperties.class);
        Mockito.when(accountProperties.getCommissionRate()).thenReturn(0.02);
        allocator = new VqsAllBackBetAllocator(accountProperties, accountAllocations);
    }

    @Test
    void sizeAllocations() {
        List<OpportunitySelection> selections = Arrays.asList(
                OpportunitySelection.of(a0, SelectionPrice.back(new PriceSize(1.26, 300.0))),
                OpportunitySelection.of(a1, SelectionPrice.back(new PriceSize(15.0, 45.0))),
                OpportunitySelection.of(a2, SelectionPrice.back(new PriceSize(18.0, 32.0))),
                OpportunitySelection.of(a3, SelectionPrice.back(new PriceSize(16.5, 4.0))));
        OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
        List<Double> allocations = allocator.sizeAllocations(opportunityBet);
        assertThat(allocations, notNullValue());
        assertThat(allocations.size(), is(4));
        assertThat(allocations.get(0), is(51.74));
        assertThat(allocations.get(1), is(4.4));
        assertThat(allocations.get(2), is(3.67));
        assertThat(allocations.get(3), is(4.0));
    }

    @Test
    void capcityCheckGreaterThanMaxAllocationSum() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            List<OpportunitySelection> selections = Arrays.asList(
                    OpportunitySelection.of(a0, SelectionPrice.back(new PriceSize(1.1, 300.0))),
                    OpportunitySelection.of(a1, SelectionPrice.back(new PriceSize(14.0, 45.0))),
                    OpportunitySelection.of(a2, SelectionPrice.back(new PriceSize(18.0, 32.0))),
                    OpportunitySelection.of(a3, SelectionPrice.back(new PriceSize(15.0, 4.0))));
            OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
            allocator.sizeAllocations(opportunityBet);
        });
        assertTrue(exception.getMessage().contains("capacityCheck=67.02003642987249 greater than opportunityMaxAllocation=67.0 ...aborting plm processing"));
    }

    @Test
    void negativeOptimizedNetProfit() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            List<OpportunitySelection> selections = Arrays.asList(
                    OpportunitySelection.of(a0, SelectionPrice.back(new PriceSize(1.22, 300.0))),
                    OpportunitySelection.of(a1, SelectionPrice.back(new PriceSize(15.0, 45.0))),
                    OpportunitySelection.of(a2, SelectionPrice.back(new PriceSize(18.0, 32.0))),
                    OpportunitySelection.of(a3, SelectionPrice.back(new PriceSize(15.0, 4.0))));
            OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
            allocator.sizeAllocations(opportunityBet);
        });
        assertTrue(exception.getMessage().contains("optimizedNetProfit=-0.9668989388783619 is lessThan or equal to Zero ...aborting plm processing"));
    }

    @Test
    void negativeEResized() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            List<OpportunitySelection> selections = Arrays.asList(
                    OpportunitySelection.of(a0, SelectionPrice.back(new PriceSize(1.26, 300.0))),
                    OpportunitySelection.of(a1, SelectionPrice.back(new PriceSize(15.0, 45.0))),
                    OpportunitySelection.of(a2, SelectionPrice.back(new PriceSize(18.0, 32.0))),
                    OpportunitySelection.of(a3, SelectionPrice.back(new PriceSize(15.0, 4.0))));
            OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
            allocator.sizeAllocations(opportunityBet);
        });
        assertTrue(exception.getMessage().contains("eResized=-0.2850079999999977 is lessThan or equal to Zero ...aborting plm processing"));
    }
}