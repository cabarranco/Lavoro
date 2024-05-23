package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.esa.cache.market.MarketRunnerSnap;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.inplay.model.MatchScore;
import com.asbresearch.betfair.ref.entities.Event;
import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.*;
import com.asbresearch.pulse.service.MarketSelection;
import com.asbresearch.pulse.service.MarketSnaps;
import com.asbresearch.pulse.service.OpportunityQueue;
import com.asbresearch.pulse.service.SelectionPrice;
import com.betfair.esa.swagger.model.MarketDefinition;
import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.asbresearch.pulse.model.StrategyRule.SUM_OF_IMPLIED_PROBABILITY;
import static com.asbresearch.pulse.model.StrategyRule.splParser;
import static com.asbresearch.pulse.util.Constants.ODD;
import static com.asbresearch.pulse.util.Constants.OPPORTUNITY_ID;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
public class StrategyCriteriaEvaluator {
    private final StrategySpec strategySpec;
    private final Set<MarketSelection> selections;
    private final Map<MarketSelection, SelectionPrice> selectionPrices = new ConcurrentHashMap<>();
    private final Map<String, MarketSelection> ruleRunnerCodes2selection = new ConcurrentHashMap<>();
    private final Event event;
    private final BetfairInPlayService inPlayService;
    private final MarketSnaps marketSnaps;
    private final OpportunityQueue opportunityQueue;
    private final StrategyEventIgnoreContainer strategyEventIgnoreContainer;

    public StrategyCriteriaEvaluator(StrategySpec strategySpec,
                                     Set<MarketSelection> selections,
                                     Event event,
                                     BetfairInPlayService inPlayService,
                                     MarketSnaps marketSnaps,
                                     OpportunityQueue opportunityBets,
                                     StrategyEventIgnoreContainer strategyEventIgnoreContainer) {

        Preconditions.checkNotNull(opportunityBets, "opportunityBets queue must be provided");
        this.opportunityQueue = opportunityBets;
        this.strategySpec = strategySpec;
        this.selections = selections;
        this.event = event;
        this.inPlayService = inPlayService;
        this.marketSnaps = marketSnaps;
        selections.forEach(marketSelection -> selectionPrices.put(marketSelection, SelectionPrice.NULL));
        selections.forEach(marketSelection -> ruleRunnerCodes2selection.put(marketSelection.getUserRunnerCode().getCode(), marketSelection));
        this.strategyEventIgnoreContainer = strategyEventIgnoreContainer;
    }

    public void execute(String marketId) {
        log.debug("Starting criteria evaluation");
        if (anySelectionPriceChange()) {
            boolean inPlay = isInPlay(marketId);
            if (strategySpec.getEventCriteria().isPreLive() && !inPlay) {
                log.info("PreLive {}", selections);
                executeStrategyRules();
            }
            if (strategySpec.getEventCriteria().isLive() && inPlay) {
                applyInPlayCriteria();
            }
        } else {
            log.debug("No price/size change for selections");
        }
    }

    private boolean isInPlay(String marketId) {
        Optional<MarketSnap> marketSnap = marketSnaps.getMarketSnap(marketId);
        if (marketSnap.isPresent()) {
            MarketDefinition marketDefinition = marketSnap.get().getMarketDefinition();
            if (marketDefinition != null && marketDefinition.getInPlay() != null) {
                return marketDefinition.getInPlay();
            }
        }
        return inPlayService.isInPlay(Integer.valueOf(event.getId()));
    }

    private boolean anySelectionPriceChange() {
        for (MarketSelection selection : selections) {
            Optional<SelectionPrice> price = currentSelectionPrice(selection);
            if (price.isPresent()) {
                SelectionPrice currentPrice = price.get();
                SelectionPrice prevPrice = selectionPrices.put(selection, currentPrice);
                if (isValidPriceAndSize(currentPrice) && hasPriceChanged(selection, currentPrice, prevPrice)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidPriceAndSize(SelectionPrice currentPrice) {
        PriceSize back = currentPrice.getBack();
        if (back == null || back.getPrice() == null || back.getSize() == null) {
            return false;
        }
        if (back.getPrice() <= 0.0) {
            return false;
        }
        if (back.getSize() <= 0.0) {
            return false;
        }
        PriceSize lay = currentPrice.getLay();
        if (lay == null || lay.getPrice() == null || lay.getSize() == null) {
            return false;
        }
        if (lay.getPrice() <= 0.0) {
            return false;
        }
        if (lay.getSize() <= 0.0) {
            return false;
        }
        return true;
    }

    protected boolean hasPriceChanged(MarketSelection selection, SelectionPrice currentPrice, SelectionPrice prevPrice) {
        boolean result;
        if (prevPrice == SelectionPrice.NULL) {
            result = true;
        } else {
            PriceSize current;
            PriceSize prev;
            if (Side.BACK == selection.getUserRunnerCode().getSide()) {
                current = currentPrice.getBack();
                prev = prevPrice.getBack();
            } else {
                current = currentPrice.getLay();
                prev = prevPrice.getLay();
            }
            result = current != null
                    && current.getPrice() != null
                    && current.getSize() != null
                    && prev != null
                    && prev.getPrice() != null
                    && prev.getSize() != null
                    && (Double.compare(current.getPrice(), prev.getPrice()) != 0 || Double.compare(current.getSize(), prev.getSize()) != 0);
        }
        if (result) {
            log.info("Price/Vol change runnerId={} current={} prev={}",
                    selection.getRunnerCatalog().getSelectionId(),
                    currentPrice,
                    prevPrice);
        }
        return result;
    }

    protected Optional<SelectionPrice> currentSelectionPrice(MarketSelection marketSelection) {
        Optional<MarketSnap> marketSnap = marketSnaps.getMarketSnap(marketSelection.getMarketId());
        if (marketSnap.isPresent()) {
            Optional<MarketRunnerSnap> selectionSnap = marketSnap.get().getMarketRunners()
                    .stream()
                    .filter(marketRunnerSnap -> marketSelection.getRunnerCatalog().getSelectionId().equals(marketRunnerSnap.getRunnerId().getSelectionId()))
                    .findAny();
            log.debug("selection={} snap={}", marketSelection, selectionSnap.orElse(null));
            if (selectionSnap.isPresent()) {
                MarketRunnerSnap marketRunnerSnap = selectionSnap.get();
                List<LevelPriceSize> bdatb = marketRunnerSnap.getPrices().getBdatb();
                List<LevelPriceSize> bdatl = marketRunnerSnap.getPrices().getBdatl();
                if (!bdatb.isEmpty() && !bdatl.isEmpty()) {
                    return Optional.of(SelectionPrice.of(bdatb.get(0), bdatl.get(0)));
                }
            }
        }
        return Optional.empty();
    }

    private void applyInPlayCriteria() {
        Instant now = Instant.now();
        StrategyPeriod strategyPeriod = strategyPeriodForEvent(event.getId(), strategySpec.getEventCriteria());
        boolean strategyInScope = strategyPeriod.isBetweenCurrentTime(now);
        log.info("inPlay=true currentTime={} strategyPeriodStart={} strategyPeriodEnd={} strategyInScope={} event={}",
                now, strategyPeriod.getStart(), strategyPeriod.getEnd(), strategyInScope, event.getId());
        if (strategyPeriod.getStart() == null || strategyPeriod.getEnd() == null) {
            log.error("Cant apply inPlay criteria inPlay=true strategyPeriod={} criteria={} strategy={}", strategyPeriod, strategySpec.getEventCriteria(), strategySpec.getStrategyId());
            return;
        }
        if (now.isAfter(strategyPeriod.getEnd())) {
            strategyEventIgnoreContainer.ignoreEventForStrat(strategySpec.getStrategyId(), event.getId());
            return;
        }
        if (strategyInScope) {
            applyScores();
        }
    }

    protected StrategyPeriod strategyPeriodForEvent(String eventId, EventCriteria eventCriteria) {
        StrategyPeriod.StrategyPeriodBuilder period = StrategyPeriod.builder();
        Optional<Instant> kickOffTime = inPlayService.kickOffTime(Integer.valueOf(eventId));
        if (kickOffTime.isEmpty() || kickOffTime.get().equals(Instant.EPOCH)) {
            log.error("inPlay=true but can't get kickOffTime for eventId={}", eventId);
            return period.build();
        }
        log.debug("StrategyPeriod.kickOffTime={}", kickOffTime.get());
        period.start(kickOffTime.get().plus(eventCriteria.getStartFromKickOff(), MINUTES));
        period.end(kickOffTime.get().plus(eventCriteria.getEndFromKickOff(), MINUTES));

        return period.build();
    }

    private void applyScores() {
        Optional<MatchScore> matchScoreOptional = inPlayService.score(Integer.valueOf(event.getId()));
        if (matchScoreOptional.isPresent()) {
            MatchScore matchScore = matchScoreOptional.get();
            log.info("Begin applying scores currentScore={} previousScore={} currentScoreSpec={} prevScoreSpec={}",
                    matchScore.currentScore(),
                    matchScore.previousScore(),
                    strategySpec.getEventCriteria().getCurrentLiveScores(),
                    strategySpec.getEventCriteria().getPreviousScore());
            if (strategySpec.getEventCriteria().getPreviousScore() != null) {
                String previousScoreCriteria = strategySpec.getEventCriteria().getPreviousScore();
                String currentScoreCriteria = strategySpec.getEventCriteria().getCurrentLiveScores().iterator().next();
                log.info("Checking previousScore={} currentScore={}", matchScore.previousScore(), matchScore.currentScore());
                if (previousScoreCriteria.equals(matchScore.previousScore()) && currentScoreCriteria.equals(matchScore.currentScore())) {
                    log.info("Live triggering prevScore={} {}", matchScore.previousScore(), selections);
                    executeStrategyRules();
                }
            } else if (!isEmpty(strategySpec.getEventCriteria().getCurrentLiveScores())) {
                String currentScore = matchScore.currentScore();
                if (strategySpec.getEventCriteria().getCurrentLiveScores().contains(currentScore)) {
                    log.info("Live triggering currentScore={} criteria={}", currentScore, strategySpec.getEventCriteria().getCurrentLiveScores());
                    executeStrategyRules();
                }
            } else {
                log.info("Live triggering allScores {}", selections);
                executeStrategyRules();
            }
        } else {
            log.error("Missing match score");
        }
        log.info("End applying scores");
    }

    private Map<String, Double> mapRuleVarsToDouble() {
        Map<String, Double> result = new HashMap<>();
        strategySpec.getStrategyCriteria().getRules().forEach(strategyRule -> {
            strategyRule.getVars().forEach(var -> {
                if (!result.containsKey(var)) {
                    String key = String.format("%s.%s", var, strategyRule.getType());
                    if (SUM_OF_IMPLIED_PROBABILITY.equals(var)) {
                        result.put(key, sumOfImpliedProbability());
                    } else {
                        MarketSelection marketSelection = ruleRunnerCodes2selection.get(var);
                        if (marketSelection != null) {
                            Optional<SelectionPrice> price = currentSelectionPrice(marketSelection);
                            if (price.isPresent()) {
                                PriceSize priceSize;
                                if (Side.BACK == marketSelection.getUserRunnerCode().getSide()) {
                                    priceSize = price.get().getBack();
                                } else {
                                    priceSize = price.get().getLay();
                                }
                                if (ODD.equals(strategyRule.getType())) {
                                    result.put(key, priceSize.getPrice());
                                } else {
                                    result.put(key, priceSize.getSize());
                                }
                            }
                        }
                    }
                }
            });
        });
        return result;
    }

    private void executeStrategyRules() {
        boolean satisfyCriteria = false;
        Map<String, Double> ruleVars2ToDouble = mapRuleVarsToDouble();
        for (StrategyRule strategyRule : strategySpec.getStrategyCriteria().getRules()) {
            String expression = strategyRule.getExpr();
            boolean canMapAllVars = true;
            for (String var : strategyRule.getVars()) {
                String key = String.format("%s.%s", var, strategyRule.getType());
                if (ruleVars2ToDouble.containsKey(key)) {
                    expression = expression.replaceAll(var, String.valueOf(ruleVars2ToDouble.get(key)));
                } else {
                    canMapAllVars = false;
                    break;
                }
            }
            if (canMapAllVars) {
                try {
                    satisfyCriteria = splParser.parseExpression(expression).getValue(Boolean.class);
                    log.info("criteria={} ruleName={} rule={} expression={}", satisfyCriteria, strategyRule.getName(), strategyRule.getExpr(), expression);
                } catch (RuntimeException e) {
                    log.warn("Error evaluating rule={}", strategyRule.getExpr(), e);
                    satisfyCriteria = false;
                }
            }
            if (!satisfyCriteria) {
                break;
            }
        }
        if (satisfyCriteria) {
            opportunityQueue.add(createOpportunityFromBookRunnersAllocator());
        }
    }

    private OpportunityBet createOpportunityFromBookRunnersAllocator() {
        Map<String, MarketSelection> code2selection = new HashMap<>();
        selections.forEach(marketSelection -> code2selection.put(marketSelection.getUserRunnerCode().getCode(), marketSelection));
        List<OpportunitySelection> opportunitySelections = strategySpec.getBookRunnersAllocator().stream()
                .map(userRunnerCode -> code2selection.get(userRunnerCode.getCode()))
                .map(selection -> new OpportunitySelection(selection, currentPriceForSelection(selection.getMarketId(), selection.getRunnerCatalog().getSelectionId())))
                .collect(Collectors.toList());
        boolean inPlay = isInPlay(opportunitySelections.iterator().next().getMarketSelection().getMarketId());
        return new OpportunityBet(event, strategySpec.getStrategyId(), Instant.now(), opportunitySelections, strategySpec.getAllocatorId(), MDC.get(OPPORTUNITY_ID), inPlay);
    }

    private Double sumOfImpliedProbability() {
        List<Double> odds = new ArrayList<>();
        for (UserRunnerCode runnerCode : strategySpec.getBookRunnersCompute()) {
            MarketSelection marketSelection = ruleRunnerCodes2selection.get(runnerCode.getCode());
            if (marketSelection != null) {
                Optional<SelectionPrice> price = currentSelectionPrice(marketSelection);
                double probability = 0.0;
                if (price.isPresent()) {
                    if (Side.BACK == marketSelection.getUserRunnerCode().getSide()) {
                        Double odd = price.get().getBack().getPrice();
                        if (odd > 0) {
                            probability = 1 / odd;
                            odds.add(probability);
                        }
                    } else {
                        Double odd = price.get().getLay().getPrice();
                        if (odd > 0) {
                            probability = 1 - (1 / odd);
                            odds.add(probability);
                        }
                    }
                    log.info("runnerCode={} probability={} price={} marketSelection={}",
                            runnerCode, probability, price.get(), marketSelection);
                }
            }
        }
        return odds.stream().mapToDouble(Double::doubleValue).sum();
    }

    private SelectionPrice currentPriceForSelection(String marketId, Long selectionId) {
        Optional<MarketSnap> marketSnap = marketSnaps.getMarketSnap(marketId);
        if (marketSnap.isPresent()) {
            Optional<MarketRunnerSnap> optional = marketSnap.get().getMarketRunners().stream()
                    .filter(marketRunnerSnap -> selectionId.equals(marketRunnerSnap.getRunnerId().getSelectionId()))
                    .findFirst();
            if (optional.isPresent()) {
                MarketRunnerSnap marketRunnerSnap = optional.get();
                PriceSize backPrice = PriceSize.from(marketRunnerSnap.getPrices().getBdatb().get(0));
                PriceSize layPrice = PriceSize.from(marketRunnerSnap.getPrices().getBdatl().get(0));
                return new SelectionPrice(backPrice, layPrice);
            }
        } else {
            log.warn("Missing MarketCache for marketId={}", marketId);
        }
        return SelectionPrice.NULL;
    }

    @Value
    @Builder(toBuilder = true)
    private static class StrategyPeriod {
        Instant start;
        Instant end;

        public boolean isBetweenCurrentTime(Instant time) {
            return (time.equals(start) || time.isAfter(start)) && (time.equals(end) || time.isBefore(end));
        }
    }
}
