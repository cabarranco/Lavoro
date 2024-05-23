package com.asbresearch.pulse.service.plm;

import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.service.strategy.Strategy;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@EnableConfigurationProperties(AccountProperties.class)
@Component
@Slf4j
public final class ConcentrationTables {
    private final ConcentrationRecords events = new ConcentrationRecords();
    private final ConcentrationRecords strategies = new ConcentrationRecords();
    private final ConcentrationRecords strategyEvents = new ConcentrationRecords();
    private final AccountProperties accountProperties;
    private final AccountAllocations accountAllocations;

    @Autowired
    public ConcentrationTables(AccountProperties accountProperties, AccountAllocations accountAllocations) {
        Preconditions.checkNotNull(accountProperties, "accountProperties must be provided");
        Preconditions.checkNotNull(accountAllocations, "accountAllocations must be provided");
        this.accountProperties = accountProperties;
        this.accountAllocations = accountAllocations;
    }

    public ConcentrationRecords getEvents() {
        return events;
    }

    public ConcentrationRecords getStrategies() {
        return strategies;
    }

    public ConcentrationRecords getStrategyEvents() {
        return strategyEvents;
    }


    public void init(Collection<Strategy> initStrategies) {
        Set<String> eventIds = initStrategies.stream()
                .map(strategy -> strategy.events())
                .flatMap(events -> events.stream())
                .map(event -> event.getId())
                .collect(Collectors.toSet());
        double startBalance = (accountAllocations.getBalanceSaving() / accountProperties.getPercentageBalanceToSave()) - accountAllocations.getBalanceSaving();
        double maxAvailableBalanceToBet = Precision.round(startBalance * accountProperties.getMaxEventConcentration(), 2);
        log.info("startBalance={} maxEventConcentration={} maxAvailableBalanceToBet={}", startBalance, accountProperties.getMaxEventConcentration(), maxAvailableBalanceToBet);
        eventIds.forEach(eventId -> events.upSert(eventId, maxAvailableBalanceToBet, 0.0));
        initStrategies.forEach(strategy -> {
            strategies.upSert(strategy.getId(), Precision.round(startBalance * accountProperties.getMaxStrategyConcentration(), 2), 0.0);
            eventIds.forEach(eventId -> strategyEvents.upSert(String.format("%s-%s", strategy.getId(), eventId), Precision.round(startBalance * accountProperties.getMaxStrategyEventConcentration(), 2), 0.0));
        });
    }


    public void updateEventConcentration(String eventId, double currentBetBalance) {
        double concentration = accountAllocations.getAvailableToBetBalance() * accountProperties.getMaxEventConcentration();
        events.upSert(eventId, Precision.round(concentration, 2), currentBetBalance);
    }

    public void updateStrategyConcentration(String strategyId, double currentBetBalance) {
        double concentration = accountAllocations.getAvailableToBetBalance() * accountProperties.getMaxStrategyConcentration();
        strategies.upSert(strategyId, Precision.round(concentration, 2), currentBetBalance);
    }

    public void updateStrategyEventConcentration(String strategyId, String eventId, double currentBetBalance) {
        double concentration = accountAllocations.getAvailableToBetBalance() * accountProperties.getMaxStrategyEventConcentration();
        strategyEvents.upSert(String.format("%s-%s", strategyId, eventId), Precision.round(concentration, 2), currentBetBalance);
    }

    @Override
    public String toString() {
        return String.format("ConcentrationTables{ events=%s, strategies=%s, strategyEvents=%s}", events, strategies, strategyEvents);
    }
}
