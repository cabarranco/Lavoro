package com.asbresearch.pulse.service.allocator;

import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.service.plm.AccountAllocations;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@EnableConfigurationProperties(AccountProperties.class)
public class VqsAllBackBetAllocator extends AbstractBetAllocator {

    @Autowired
    public VqsAllBackBetAllocator(AccountProperties accountProperties, AccountAllocations accountAllocations) {
        super(accountProperties, accountAllocations);
    }

    @Override
    public String name() {
        return "VQS.ALL.B";
    }

    @Override
    public List<Double> sizeAllocations(OpportunityBet opportunityBet) {
        double primaryLegOdd = primaryLegOdd(opportunityBet);
        double commissionRate = accountProperties.getCommissionRate();
        double oneMinusCommissionRate = 1 - commissionRate;
        double c1 = 1.0 / ((primaryLegOdd * oneMinusCommissionRate) + commissionRate);
        double maxAllocationSum = accountAllocations.getOpportunityMaxAllocationSum();
        double aoMin = c1 * maxAllocationSum;
        double capacityCheck = aoMin + (LEG_MIN_ALLOCATION * (opportunityBet.getSelections().size() - 1));
        if (capacityCheck > maxAllocationSum) {
            log.debug("c1={} commission={} primaryLegOdd={} aoMin={} opportunityMaxAllocationSum={} capacityCheck={} legMinAllocation={}", c1, commissionRate, primaryLegOdd, aoMin, maxAllocationSum, capacityCheck, LEG_MIN_ALLOCATION);
            throw new RuntimeException(String.format("capacityCheck=%s greater than opportunityMaxAllocation=%s ...aborting plm processing", capacityCheck, maxAllocationSum));
        }
        double pt = calcImpliedProbabilitySecondaryLegs(opportunityBet);
        double b0 = 1 + (pt * commissionRate / oneMinusCommissionRate);
        double d0 = 1 - (b0 * c1) - pt;
        double a0 = (pt / oneMinusCommissionRate) + (b0 * c1);
        double optimizedNetProfit = maxAllocationSum * (d0 / a0);
        if (optimizedNetProfit <= 0) {
            log.debug("c1={} commission={} primaryLegOdd={} aoMin={} opportunityMaxAllocationSum={} capacityCheck={} legMinAllocation={} pt={} b0={} d0={} a0={} optimizedNetProfit={}",
                    c1, commissionRate, primaryLegOdd, aoMin, maxAllocationSum, capacityCheck, LEG_MIN_ALLOCATION, pt, b0, d0, a0, optimizedNetProfit);
            throw new RuntimeException(String.format("optimizedNetProfit=%s is lessThan or equal to Zero ...aborting plm processing", optimizedNetProfit));
        }
        double aot = aoMin + (optimizedNetProfit * c1);
        double a1 = maxAllocationSum + (aot * (commissionRate / oneMinusCommissionRate)) + (optimizedNetProfit / oneMinusCommissionRate);
        List<Double> allocations = calcAllocations(opportunityBet, maxAllocationSum, a1);
        double minSizeRatio = minSizeRatio(opportunityBet, allocations);
        allocations = resizeAllocations(allocations, minSizeRatio);
        double eResized = eResized(primaryLegOdd, oneMinusCommissionRate, allocations);
        if (eResized <= 0) {
            log.debug("c1={} commission={} primaryLegOdd={} aoMin={} opportunityMaxAllocationSum={} capacityCheck={} legMinAllocation={} pt={} b0={} d0={} a0={} optimizedNetProfit={} aot={} a1={} minSizeRatio={} eResized={} allocations={}",
                    c1, commissionRate, primaryLegOdd, aoMin, maxAllocationSum, capacityCheck, LEG_MIN_ALLOCATION, pt, b0, d0, a0, optimizedNetProfit, aot, a1, minSizeRatio, eResized, allocations);
            throw new RuntimeException(String.format("eResized=%s is lessThan or equal to Zero ...aborting plm processing", eResized));
        }
        log.info("c1={} commission={} primaryLegOdd={} aoMin={} opportunityMaxAllocationSum={} capacityCheck={} legMinAllocation={} pt={} b0={} d0={} a0={} optimizedNetProfit={} aot={} a1={} minSizeRatio={} eResized={} allocations={}",
                c1, commissionRate, primaryLegOdd, aoMin, maxAllocationSum, capacityCheck, LEG_MIN_ALLOCATION, pt, b0, d0, a0, optimizedNetProfit, aot, a1, minSizeRatio, eResized, allocations);
        return allocations;
    }

    private List<Double> calcAllocations(OpportunityBet opportunityBet, double maxAllocationSum, double a1) {
        List<Double> allocations = Lists.newArrayList(0.0);
        allocations.addAll(secondaryLegAllocations(opportunityBet, a1));
        double primaryAllocation = maxAllocationSum - (allocations.stream().skip(1).limit(allocations.size()).mapToDouble(Double::doubleValue).sum());
        allocations.set(0, primaryAllocation);
        return allocations;
    }
}
