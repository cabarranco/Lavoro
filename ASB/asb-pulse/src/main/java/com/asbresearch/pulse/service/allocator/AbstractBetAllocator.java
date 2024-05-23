package com.asbresearch.pulse.service.allocator;

import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import com.asbresearch.pulse.service.plm.AccountAllocations;
import com.google.common.base.Preconditions;
import org.apache.commons.math3.util.Precision;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractBetAllocator implements BetAllocator {
    protected final AccountProperties accountProperties;
    protected final AccountAllocations accountAllocations;

    public AbstractBetAllocator(AccountProperties accountProperties, AccountAllocations accountAllocations) {
        Preconditions.checkNotNull(accountProperties, "accountProperties must be provided");
        Preconditions.checkNotNull(accountAllocations, "accountAllocations must be provided");

        this.accountProperties = accountProperties;
        this.accountAllocations = accountAllocations;
    }

    protected List<Double> calcImpliedProbabilities(OpportunityBet opportunityBet) {
        return opportunityBet.getSelections()
                .stream()
                .map(this::calcImpliedProbabilities)
                .collect(Collectors.toList());
    }

    protected Double calcImpliedProbabilities(OpportunitySelection selection) {
        Side side = selection.getMarketSelection().getUserRunnerCode().getSide();
        if (Side.BACK == side) {
            return 1.0 / selection.getSelectionPrice().getBack().getPrice();
        }
        return 1.0 - (1.0 / selection.getSelectionPrice().getLay().getPrice());
    }

    protected double primaryLegOdd(OpportunityBet opportunityBet) {
        OpportunitySelection selection = opportunityBet.getSelections().iterator().next();
        if (Side.BACK == selection.getMarketSelection().getUserRunnerCode().getSide()) {
            return selection.getSelectionPrice().getBack().getPrice();
        }
        return selection.getSelectionPrice().getLay().getPrice();
    }

    protected Double calcImpliedProbabilitySecondaryLegs(OpportunityBet opportunityBet) {
        return opportunityBet.getSelections()
                .stream()
                .skip(1)
                .limit(opportunityBet.getSelections().size())
                .map(this::calcImpliedProbabilities)
                .mapToDouble(value -> value)
                .sum();
    }

    protected double getLegSize(OpportunityBet opportunityBet, int index) {
        OpportunitySelection selection = opportunityBet.getSelections().get(index);
        Side side = selection.getMarketSelection().getUserRunnerCode().getSide();
        if (Side.BACK == side) {
            return selection.getSelectionPrice().getBack().getSize();
        }
        return selection.getSelectionPrice().getLay().getSize();
    }

    protected double minSizeRatio(OpportunityBet opportunityBet, List<Double> allocations) {
        return IntStream.range(0, allocations.size()).mapToObj(i -> getLegSize(opportunityBet, i) / allocations.get(i)).mapToDouble(value -> value).min().getAsDouble();
    }

    protected List<Double> resizeAllocations(List<Double> allocations, double minSizeRatio) {
        return allocations.stream()
                .map(allocation -> minSizeRatio < 1 ? Precision.round(Math.max(allocation * minSizeRatio, LEG_MIN_ALLOCATION), 2) : Precision.round(allocation, 2))
                .collect(Collectors.toList());
    }

    protected List<Double> secondaryLegAllocations(OpportunityBet opportunityBet, double a1) {
        List<Double> impliedProbabilities = calcImpliedProbabilities(opportunityBet);
        return impliedProbabilities.stream()
                .skip(1)
                .limit(impliedProbabilities.size())
                .map(impliedProbability -> Math.max(LEG_MIN_ALLOCATION, a1 * impliedProbability))
                .collect(Collectors.toList());
    }

    protected double eResized(double primaryLegOdd, double oneMinusCommissionRate, List<Double> allocations) {
        return (allocations.get(0) * primaryLegOdd * oneMinusCommissionRate) - allocations.stream().mapToDouble(value -> value).sum();
    }

}
