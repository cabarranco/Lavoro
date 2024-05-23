package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.BetfairEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.asbresearch.pulse.util.Constants.timeRangeForTradingDay;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Slf4j
public abstract class AbstractStrategyProvider implements StrategyProvider {

    private final BetfairEventService betfairEventService;
    private final BetfairMarketTypeMapping marketTypeMapping;
    private final BetfairInPlayService betfairInPlayService;
    private final StrategyProperties strategyProperties;
    private final StrategyEventIgnoreContainer strategyEventIgnoreContainer;

    @Autowired
    public AbstractStrategyProvider(
            BetfairEventService betfairEventService,
            BetfairMarketTypeMapping marketTypeMapping,
            BetfairInPlayService BetfairInPlayService,
            StrategyProperties strategyProperties,
            StrategyEventIgnoreContainer strategyEventIgnoreContainer,
            AppProperties appProperties) {

        checkNotNull(appProperties, "appProperties must be provided");
        checkNotNull(appProperties.getNode(), "appProperties.node must be provided");
        checkArgument(!appProperties.getNode().isEmpty(), "appProperties.node must not be empty");

        this.strategyProperties = strategyProperties;
        this.betfairEventService = betfairEventService;
        this.marketTypeMapping = marketTypeMapping;
        this.betfairInPlayService = BetfairInPlayService;
        this.strategyEventIgnoreContainer = strategyEventIgnoreContainer;
    }

    @Override
    public List<Strategy> getCurrentStrategies() {
        List<StrategySpec> strategySpecs = getCurrentStrategySpec();
        List<Strategy> strategies = strategySpecs.stream()
                .map(strategySpec -> new DefaultStrategy(strategySpec,
                        betfairEventService,
                        marketTypeMapping,
                        betfairInPlayService,
                        timeRangeForTradingDay(strategyProperties.getTradingHours()),
                        strategyEventIgnoreContainer))
                .collect(Collectors.toList());
        strategies.forEach(strategy -> strategy.init(betfairEventService));
        strategies = strategies.stream().filter(strategy -> !strategy.marketSubscriptions().isEmpty()).collect(toImmutableList());
        log.info("Loaded {} strategies on {} {}", strategies.size(), LocalDate.now(), strategies.stream().map(strategy -> strategy.getId()).collect(Collectors.toList()));
        return strategies;
    }
}
