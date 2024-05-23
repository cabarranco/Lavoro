package com.asbresearch.pulse.service.allocator;

import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import com.asbresearch.pulse.service.MarketSelection;
import com.asbresearch.pulse.service.plm.AccountAllocations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.asbresearch.pulse.service.SelectionPrice.back;
import static com.asbresearch.pulse.service.SelectionPrice.lay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VqsMainLayBetAllocatorTest {
    private Event event;
    private MarketSelection a0;
    private MarketSelection a1;
    private MarketSelection a2;
    private VqsMainLayBetAllocator allocator;

    @BeforeEach
    void setUp() {
        event = new Event("id", "name", "countryCode", "timeZone", Instant.now());
        a0 = MarketSelection.of(event, null, null, null, new UserRunnerCode("MO.D.L"));
        a1 = MarketSelection.of(event, null, null, null, new UserRunnerCode("CS.00.B"));
        a2 = MarketSelection.of(event, null, null, null, new UserRunnerCode("CS.11.B"));
        AccountAllocations accountAllocations = Mockito.mock(AccountAllocations.class);
        Mockito.when(accountAllocations.getOpportunityMaxAllocationSum()).thenReturn(67.0);
        AccountProperties accountProperties = Mockito.mock(AccountProperties.class);
        Mockito.when(accountProperties.getCommissionRate()).thenReturn(0.02);
        allocator = new VqsMainLayBetAllocator(accountProperties, accountAllocations);
    }

    @Test
    void sizeAllocations() {
        List<OpportunitySelection> selections = Arrays.asList(
                OpportunitySelection.of(a0, lay(new PriceSize(3.3, 300.0))),
                OpportunitySelection.of(a1, back(new PriceSize(12.5, 45.0))),
                OpportunitySelection.of(a2, back(new PriceSize(7.2, 10.0))));
        OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
        List<Double> allocations = allocator.sizeAllocations(opportunityBet);
        assertThat(allocations, notNullValue());
        assertThat(allocations.size(), is(3));
        assertThat(allocations.get(0), is(21.61));
        assertThat(allocations.get(1), is(5.76));
        assertThat(allocations.get(2), is(10.0));
    }

    @Test
    void capacityCheckGreaterThanZero() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            List<OpportunitySelection> selections = Arrays.asList(
                    OpportunitySelection.of(a0, lay(new PriceSize(4.5, 300.0))),
                    OpportunitySelection.of(a1, back(new PriceSize(12.5, 45.0))),
                    OpportunitySelection.of(a2, back(new PriceSize(7.2, 10.0))));
            OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
            allocator.sizeAllocations(opportunityBet);
        });
        assertTrue(exception.getMessage().contains("capacityCheck=1.1410192425973236 greater than zero ...aborting plm processing"));
    }

    @Test
    void eResizedLessThanOrEqualZero() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            List<OpportunitySelection> selections = Arrays.asList(
                    OpportunitySelection.of(a0, lay(new PriceSize(3.3, 4.0))),
                    OpportunitySelection.of(a1, back(new PriceSize(12.5, 45.0))),
                    OpportunitySelection.of(a2, back(new PriceSize(7.2, 10.0))));
            OpportunityBet opportunityBet = new OpportunityBet(event, "STRAT", Instant.now(), selections, "allocatorId", null, true);
            allocator.sizeAllocations(opportunityBet);
        });
        assertTrue(exception.getMessage().contains("eResized=-0.08000000000000007 is lessThan or equal to Zero ...aborting plm processing"));
    }
}