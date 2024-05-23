package com.asbresearch.pulse.service;

import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

@EnableConfigurationProperties(AppProperties.class)
@Component
@Slf4j
public class OpportunityQueue {
    private final ArrayBlockingQueue<OpportunityBet> opportunityBetsQueue;

    public OpportunityQueue(AppProperties appProperties) {
        opportunityBetsQueue = new ArrayBlockingQueue<>(appProperties.getOpportunityQueueCapacity());
    }

    public List<OpportunityBet> availableOpportunities(List<OpportunityBet> previous) {
        List<OpportunityBet> waitingBets = new ArrayList<>();
        opportunityBetsQueue.drainTo(waitingBets);
        log.debug("drained {} opportunityBets from queue", waitingBets.size());
        waitingBets.addAll(previous);
        Map<String, Queue<OpportunityBet>> opportunityBetsMapping = new HashMap<>();
        waitingBets.forEach(opportunityBet -> {
            Queue<OpportunityBet> priorityBlockingQueue = new PriorityQueue<>(11, comparing(OpportunityBet::getTimeStamp, reverseOrder()));
            String key = String.format("%s-%s", opportunityBet.getEvent().getId(), opportunityBet.getStrategyId());
            opportunityBetsMapping.putIfAbsent(key, priorityBlockingQueue);
            opportunityBetsMapping.get(key).add(opportunityBet);
        });
        return opportunityBetsMapping.values().stream().map(opportunityBets -> opportunityBets.peek()).collect(Collectors.toList());
    }

    public void add(OpportunityBet opportunityBet) {
        while (!opportunityBetsQueue.offer(opportunityBet)) {
            opportunityBetsQueue.remove();
        }
    }

    public OpportunityBet nextAvailableOpportunity() throws InterruptedException {
        return opportunityBetsQueue.poll(1, TimeUnit.SECONDS);
    }

    public void add(List<OpportunityBet> opportunityBets) {
        if (opportunityBets != null) {
            opportunityBets.forEach(opportunityBet -> add(opportunityBet));
        }
    }
}
