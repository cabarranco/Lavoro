package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.*;
import com.asbresearch.pulse.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.betfair.esa.swagger.model.MarketDefinition.StatusEnum.OPEN;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.*;

@Slf4j
public class DefaultStrategy implements Strategy {
    private final StrategySpec strategySpec;
    private final BetfairEventService betfairEventService;
    private List<Event> events;
    private final Map<String, StrategyCriteriaEvaluator> strategyExecutors = new ConcurrentHashMap<>();
    private final Map<MarketSelection, SelectionPrice> marketSelections = new ConcurrentHashMap<>();
    private Map<String, Set<MarketSelection>> selectionsPerMarket;
    private Map<Event, Set<MarketSelection>> selectionsPerEvent;
    private final BetfairInPlayService BetfairInPlayService;
    private final BetfairMarketTypeMapping marketTypeMapping;
    private final List<UserRunnerCode> userRunnerCodes;
    private final TimeRange tradingDayTimeRange;
    private final StrategyEventIgnoreContainer strategyEventIgnoreContainer;

    public DefaultStrategy(StrategySpec strategySpec,
                           BetfairEventService betfairEventService,
                           BetfairMarketTypeMapping marketTypeMapping,
                           BetfairInPlayService BetfairInPlayService,
                           StrategyEventIgnoreContainer strategyEventIgnoreContainer) {
        this(strategySpec, betfairEventService, marketTypeMapping, BetfairInPlayService, Constants.DEFAULT_TRADING_TIME_RANGE, strategyEventIgnoreContainer);
    }

    public DefaultStrategy(StrategySpec strategySpec,
                           BetfairEventService betfairEventService,
                           BetfairMarketTypeMapping marketTypeMapping,
                           BetfairInPlayService BetfairInPlayService,
                           TimeRange tradingDayTimeRange,
                           StrategyEventIgnoreContainer strategyEventIgnoreContainer) {
        checkNotNull(strategySpec, "StrategySpec is required");
        checkNotNull(betfairEventService, "BetfairEventService is required");
        checkNotNull(marketTypeMapping, "MarketTypeMapping is required");
        checkNotNull(BetfairInPlayService, "BetfairInPlayService is required");
        checkNotNull(tradingDayTimeRange, "TradingDayTimeRange is required");
        checkNotNull(strategyEventIgnoreContainer, "StrategyEventIgnoreContainer is required");

        this.tradingDayTimeRange = tradingDayTimeRange;
        this.strategySpec = strategySpec;
        this.betfairEventService = betfairEventService;
        this.marketTypeMapping = marketTypeMapping;
        this.userRunnerCodes = strategySpec.getBookRunnersCompute();
        this.BetfairInPlayService = BetfairInPlayService;
        this.strategyEventIgnoreContainer = strategyEventIgnoreContainer;
    }

    protected <T> Map<T, Set<MarketSelection>> groupSelectionsBy(Set<MarketSelection> marketSelections, Function<MarketSelection, T> classifier) {
        return marketSelections.stream().collect(groupingBy(classifier, Collectors.toUnmodifiableSet()));
    }

    @Override
    public void init(BetfairEventService betfairEventService) {
        events = betfairEventService.getAllEvents(tradingDayTimeRange,
                strategySpec.getEventCriteria().getType(),
                strategySpec.getEventCriteria().getIncludeCompetitions(),
                strategySpec.getEventCriteria().getExcludeCompetitions());
        if (!events.isEmpty()) {
            List<MarketSelection> selections = marketTypeMapping.getMarketSelections(userRunnerCodes, getMarketCatalogues());
            selections.forEach(marketSelection -> marketSelections.put(marketSelection, SelectionPrice.NULL));
            selectionsPerMarket = groupSelectionsBy(marketSelections.keySet(), MarketSelection::getMarketId);
            selectionsPerEvent = groupSelectionsBy(marketSelections.keySet(), MarketSelection::getEvent);
            removeEventsNotSatisfyBookRunnersSpecs();
            events = List.copyOf(events);
            selectionsPerMarket = Collections.unmodifiableMap(selectionsPerMarket);
            selectionsPerEvent = Collections.unmodifiableMap(selectionsPerEvent);
        }
        log.info("StrategyEvents Total:{} StrategyId:{} {}", events.size(), strategySpec.getStrategyId(), events.stream().map(event -> event.getId()).collect(toList()));
    }

    private void removeEventsNotSatisfyBookRunnersSpecs() {
        List<Event> eventsToRemove = new ArrayList<>();
        selectionsPerEvent.forEach((e, s) -> {
            if (s.size() != userRunnerCodes.size()) {
                log.info("Removing event={}  from strat={} markets={} userRunnerCodes={} runners={} selections={}", e.getName(), strategySpec.getStrategyId(), s.size(), userRunnerCodes.size(), userRunnerCodes, s);
                eventsToRemove.add(e);
            }
        });
        events.removeAll(eventsToRemove);
        Set<MarketSelection> selectionsToRemove = new HashSet<>();
        eventsToRemove.forEach(event -> selectionsToRemove.addAll(selectionsPerEvent.remove(event)));
        selectionsToRemove.forEach(marketSelection -> {
            selectionsPerMarket.remove(marketSelection.getMarketId());
            marketSelections.remove(marketSelection);
        });

        log.info("{} events removed from strat={} failing runners-book spec events={}", eventsToRemove.size(), strategySpec.getStrategyId(), eventsToRemove);
    }

    private List<MarketCatalogue> getMarketCatalogues() {
        Set<String> eventIds = events.stream().map(Event::getId).collect(toSet());
        Set<String> marketTypeCodes = strategySpec.getBookRunnersCompute().stream().map(UserRunnerCode::getMarket).collect(toSet());
        return betfairEventService.getMarketCatalogues(eventIds, marketTypeCodes);
    }

    @Override
    public void marketSnapsAndOpportunityQueue(MarketSnaps marketSnaps, OpportunityQueue opportunityQueue) {
        checkNotNull(marketSnaps, "Market snaps be provided");
        selectionsPerEvent.entrySet().forEach(entry -> {
            StrategyCriteriaEvaluator strategyCriteriaEvaluator = new StrategyCriteriaEvaluator(strategySpec,
                    entry.getValue(),
                    entry.getKey(),
                    BetfairInPlayService,
                    marketSnaps,
                    opportunityQueue,
                    strategyEventIgnoreContainer);
            for (MarketSelection marketSelection : entry.getValue()) {
                strategyExecutors.put(marketSelection.getMarketId(), strategyCriteriaEvaluator);
            }
        });
    }

    @Override
    public Set<String> marketSubscriptions() {
        return marketSelections.keySet().stream().map(marketSelection -> marketSelection.getMarketId()).collect(toSet());
    }

    @Override
    public void onMarketChange(MarketSnap marketSnap) {
        if (marketSnap != null && OPEN == marketSnap.getMarketDefinition().getStatus()) {
            try {
                log.debug("Begin OnMarketChange marketId={}", marketSnap.getMarketId());
                log.debug("marketSnap->{}", toSingleLineText(marketSnap));
                StrategyCriteriaEvaluator strategyCriteriaEvaluator = strategyExecutors.get(marketSnap.getMarketId());
                if (strategyCriteriaEvaluator != null) {
                    strategyCriteriaEvaluator.execute(marketSnap.getMarketId());
                }
            } finally {
                Long duration = null;
                String startTimeInTxt = MDC.get(Client.START_TIME);
                if (startTimeInTxt != null) {
                    duration = System.currentTimeMillis() - Long.valueOf(startTimeInTxt);
                }
                log.info("End OnMarketChange time={}ms", duration);
            }
        }
    }

    @Override
    public List<Event> events() {
        return events;
    }

    private String toSingleLineText(Object object) {
        if (object != null) {
            return object.toString().replaceAll("\n\r", " ");
        }
        return "";
    }

    @Override
    public void shutDown() {
    }

    @Override
    public String getId() {
        return strategySpec.getStrategyId();
    }

    @Override
    public StrategySpec getStrategySpec() {
        return strategySpec;
    }
}
