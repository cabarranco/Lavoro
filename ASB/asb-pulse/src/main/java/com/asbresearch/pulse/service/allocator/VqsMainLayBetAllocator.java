package com.asbresearch.pulse.service.allocator;

import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.service.plm.AccountAllocations;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(AccountProperties.class)
public class VqsMainLayBetAllocator extends AbstractBetAllocator {

    @Autowired
    public VqsMainLayBetAllocator(AccountProperties accountProperties, AccountAllocations accountAllocations) {
        super(accountProperties, accountAllocations);
    }

    @Override
    public String name() {
        return "VQS.MAIN.L";
    }

    @Override
    public List<Double> sizeAllocations(OpportunityBet opportunityBet) {
        double pt = calcImpliedProbabilitySecondaryLegs(opportunityBet);
        double primaryLegOdd = primaryLegOdd(opportunityBet);
        double commissionRate = accountProperties.getCommissionRate();
        double primaryLegMinusOne = primaryLegOdd - 1;
        double a0 = (primaryLegOdd - commissionRate) / primaryLegMinusOne;
        double oneMinusCommissionRate = 1 - commissionRate;
        double p0 = pt / oneMinusCommissionRate;
        double b0 = p0 * a0;
        double c0 = 1 + b0 + (p0 * commissionRate);
        double maxAllocationSum = accountAllocations.getOpportunityMaxAllocationSum();
        double tcs = Math.max(maxAllocationSum * (b0 / c0), LEG_MIN_ALLOCATION * (opportunityBet.getSelections().size() - 1));
        double capacityCheck = (primaryLegOdd * tcs) - (maxAllocationSum * oneMinusCommissionRate);
        if (capacityCheck > 0) {
            log.debug("primaryLegOdd={} maxAllocationSum={} minAllocation={} pt={} a0={} p0={} b0={} c0={} tcs={} capacityCheck={}", primaryLegOdd, maxAllocationSum, LEG_MIN_ALLOCATION, pt, a0, p0, b0, c0, tcs, capacityCheck);
            throw new RuntimeException(String.format("capacityCheck=%s greater than zero ...aborting plm processing", capacityCheck));
        }
        double A0 = Precision.round((maxAllocationSum - tcs) / primaryLegMinusOne, 2);
        Double optimizedNetProfit = (A0 * oneMinusCommissionRate) - tcs;
        if (optimizedNetProfit <= 0) {
            log.debug("primaryLegOdd={} maxAllocationSum={} minAllocation={} pt={} a0={} p0={} b0={} c0={} tcs={} capacityCheck={} A0={} optimizedNetProfit={}", primaryLegOdd, maxAllocationSum, LEG_MIN_ALLOCATION, pt, a0, p0, b0, c0, tcs, capacityCheck, A0, optimizedNetProfit);
            throw new RuntimeException(String.format("optimizedNetProfit=%s lessThanOrEqual zero ...aborting plm processing", optimizedNetProfit));
        }
        double a1 = (maxAllocationSum - (tcs * commissionRate) + optimizedNetProfit) / oneMinusCommissionRate;
        List<Double> allocations = Lists.newArrayList(A0);
        allocations.addAll(secondaryLegAllocations(opportunityBet, a1));
        double minSizeRatio = minSizeRatio(opportunityBet, allocations);
        allocations = resizeAllocations(allocations, minSizeRatio);
        double eResized = eResized(oneMinusCommissionRate, allocations);
        log.info("primaryLegOdd={} maxAllocationSum={} minAllocation={} pt={} a0={} p0={} b0={} c0={} tcs={} capacityCheck={} A0={} optimizedNetProfit={} a1={} minSizeRatio={} eResized={} allocations={}",
                primaryLegOdd, maxAllocationSum, LEG_MIN_ALLOCATION, pt, a0, p0, b0, c0, tcs, capacityCheck, A0, optimizedNetProfit, a1, minSizeRatio, eResized, allocations);
        if (eResized <= 0) {
            throw new RuntimeException(String.format("eResized=%s is lessThan or equal to Zero ...aborting plm processing", eResized));
        }
        return allocations;
    }

    private double eResized(double oneMinusCommissionRate, List<Double> allocations) {
        double sumOfSecondaryLegs = allocations.stream()
                .skip(1)
                .limit(allocations.size())
                .mapToDouble(value -> value)
                .sum();
        return (allocations.get(0) * oneMinusCommissionRate) - sumOfSecondaryLegs;
    }

}
