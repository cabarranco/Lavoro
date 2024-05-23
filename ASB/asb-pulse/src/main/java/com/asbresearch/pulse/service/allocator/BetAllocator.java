package com.asbresearch.pulse.service.allocator;

import com.asbresearch.pulse.model.OpportunityBet;
import java.util.List;

public interface BetAllocator {
    int LEG_MIN_ALLOCATION = 2;

    String name();

    List<Double> sizeAllocations(OpportunityBet opportunityBet);
}
