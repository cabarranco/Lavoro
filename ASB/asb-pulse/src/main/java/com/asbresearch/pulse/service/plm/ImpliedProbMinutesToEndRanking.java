package com.asbresearch.pulse.service.plm;

import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunitySelection;
import com.asbresearch.pulse.util.Constants;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImpliedProbMinutesToEndRanking {
    private final List<OpportunityBet> opportunityBets;
    private final Instant currentTime;

    public ImpliedProbMinutesToEndRanking(Instant currentTime, List<OpportunityBet> opportunityBets) {
        Preconditions.checkNotNull(currentTime, "instant must be provided");
        Preconditions.checkNotNull(opportunityBets, "opportunityBets must be provided");
        Preconditions.checkArgument(!opportunityBets.isEmpty(), "opportunityBets must not be empty");
        this.opportunityBets = opportunityBets;
        this.currentTime = currentTime;
    }

    public List<OpportunityBet> rank() {
        List<OpportunityBet> result = new ArrayList<>();
        Queue<OpportunityBet> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(this::impliedProbAndMinsToEndRank).reversed());
        priorityQueue.addAll(opportunityBets);
        result.add(priorityQueue.remove());
        priorityQueue.forEach(opportunityBet -> result.add(opportunityBet));
        log.info("Returning the rank by priority {}", result);
        return result;
    }

    private double impliedProbAndMinsToEndRank(OpportunityBet opportunityBet) {
        OpportunitySelection opportunitySelection = opportunityBet.getSelections().get(0);
        Side side = opportunitySelection.getMarketSelection().getUserRunnerCode().getSide();
        double impliedProbability;
        if (Side.BACK == side) {
            impliedProbability = 1 / opportunitySelection.getSelectionPrice().getBack().getPrice();
        } else {
            impliedProbability = 1 - (1 / opportunitySelection.getSelectionPrice().getLay().getPrice());
        }
        Instant gameEnd = opportunityBet.getEvent().getOpenDate().plusSeconds(Constants.GAME_DURATION_SECS);
        Duration duration = Duration.between(currentTime, gameEnd);
        log.info("impliedProbAndMinsToEndRank impliedProb={}  duration={}", impliedProbability, duration.toMinutes());
        double ranking = impliedProbability / duration.toMinutes();
        log.info("ranking={}", ranking);
        return ranking;
    }
}
