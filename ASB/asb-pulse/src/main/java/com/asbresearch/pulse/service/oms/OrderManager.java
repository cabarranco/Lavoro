package com.asbresearch.pulse.service.oms;

import com.asbresearch.betfair.esa.cache.market.MarketRunnerPrices;
import com.asbresearch.betfair.esa.cache.market.MarketRunnerSnap;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.inplay.model.MatchScore;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.LimitOrder;
import com.asbresearch.betfair.ref.entities.PlaceExecutionReport;
import com.asbresearch.betfair.ref.entities.PlaceInstruction;
import com.asbresearch.betfair.ref.entities.PlaceInstructionReport;
import com.asbresearch.betfair.ref.enums.ExecutionReportStatus;
import com.asbresearch.betfair.ref.enums.OrderType;
import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.OmsProperties;
import com.asbresearch.pulse.model.*;
import com.asbresearch.pulse.service.MarketSnaps;
import com.asbresearch.pulse.service.SelectionPrice;
import com.asbresearch.pulse.service.allocator.BetAllocator;
import com.asbresearch.pulse.service.audit.LogEntryAuditService;
import com.asbresearch.pulse.service.strategy.Strategy;
import com.asbresearch.pulse.service.strategy.StrategyCache;
import com.betfair.esa.swagger.model.MarketDefinition;
import com.betfair.esa.swagger.model.RunnerDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.asbresearch.pulse.service.SelectionPrice.getLayLimitPrice;
import static com.asbresearch.pulse.util.Constants.*;
import static com.asbresearch.pulse.util.ThreadUtils.threadFactoryBuilder;
import static com.betfair.esa.swagger.model.RunnerDefinition.StatusEnum.ACTIVE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.*;

@EnableConfigurationProperties({OmsProperties.class})
@Slf4j
@Component
public class OrderManager {
    public static final double DEFAULT_BACK_LIMIT_PRICE = 1.01;
    private final Map<String, BetAllocator> allocators;
    private final BetfairReferenceClient betfairReferenceClient;
    private final BetfairInPlayService betfairInPlayService;
    private final StrategyCache strategyCache;
    private final MarketSnaps marketSnaps;
    private final ExecutorService worker;
    private final BigQueryService bigQueryService;
    private final EmailNotifier emailNotifier;
    private final EmailProperties emailProperties;
    private final OmsProperties omsProperties;
    private final ObjectMapper mapper;
    private final AppProperties appProperties;
    private final LogEntryAuditService logEntryAuditService;
    private final AccountProperties accountProperties;

    @Autowired
    public OrderManager(StrategyCache strategyCache,
                        BetfairInPlayService betfairInPlayService,
                        MarketSnaps marketSnaps,
                        List<BetAllocator> systemAllocators,
                        BetfairReferenceClient betfairReferenceClient,
                        OmsProperties omsProperties,
                        BigQueryService bigQueryService,
                        EmailNotifier emailNotifier,
                        EmailProperties emailProperties,
                        ObjectMapper mapper,
                        AppProperties appProperties,
                        LogEntryAuditService logEntryAuditService,
                        AccountProperties accountProperties) {

        checkNotNull(accountProperties, "accountProperties must be provided");
        checkNotNull(logEntryAuditService, "logEntryAuditService must be provided");
        checkNotNull(appProperties, "appProperties must be provided");
        checkNotNull(mapper, "mapper must be provided");
        checkNotNull(emailProperties, "emailProperties must be provided");
        checkNotNull(emailNotifier, "emailNotifier must be provided");
        checkNotNull(bigQueryService, "bigQueryService must be provided");
        checkNotNull(omsProperties, "omsProperties must be provided");
        checkNotNull(marketSnaps, "marketSnaps must be provided");
        checkNotNull(betfairInPlayService, "betfairInPlayService must be provided");
        checkNotNull(strategyCache, "strategyCache must be provided");
        checkNotNull(systemAllocators, "systemAllocators must be provided");
        checkArgument(!systemAllocators.isEmpty(), "systemAllocators must not be empty");

        this.accountProperties = accountProperties;
        this.logEntryAuditService = logEntryAuditService;
        this.appProperties = appProperties;
        this.mapper = mapper;
        this.omsProperties = omsProperties;
        this.emailNotifier = emailNotifier;
        this.emailProperties = emailProperties;
        this.bigQueryService = bigQueryService;
        this.marketSnaps = marketSnaps;
        this.strategyCache = strategyCache;
        this.betfairInPlayService = betfairInPlayService;
        Map<String, BetAllocator> allocators = new HashMap<>();
        systemAllocators.forEach(betAllocator -> allocators.put(betAllocator.name(), betAllocator));
        this.allocators = unmodifiableMap(allocators);
        this.betfairReferenceClient = betfairReferenceClient;
        worker = Executors.newFixedThreadPool(omsProperties.getThreads(), threadFactoryBuilder("oms").build());
        log.info("Loaded allocators={}", allocators.keySet());
    }

    public List<PlaceInstructionReport> placeOrders(OpportunityBet opportunityBet) {
        BetAllocator betAllocator = allocators.get(opportunityBet.getAllocatorId());
        if (betAllocator != null) {
            List<Double> allocations;
            try {
                allocations = betAllocator.sizeAllocations(opportunityBet);
            } catch (RuntimeException ex) {
                log.warn("RuntimeException occurred while calculating allocations", ex);
                return emptyList();
            }
            opportunityBet = opportunityWithAllocations(opportunityBet, allocations);
            boolean isSafetyCheckSuccessful = isSafetyCheckSuccessful(opportunityBet);
            log.info("SafetyCheck result={}", isSafetyCheckSuccessful);
            if (isSafetyCheckSuccessful) {
                if (omsProperties.isPlaceLiveOrder() && !appProperties.isSimulationMode()) {
                    List<PlaceInstructionReport> reports = placePerMarketOrdersAndCollectResponse(opportunityBet);
                    logEntryAuditService.uploadAuditAsync(opportunityBet.getOpportunityId());
                    return reports;
                } else {
                    if (appProperties.isSimulationMode()) {
                        writeSimOrders(opportunityBet);
                    } else {
                        writeOpportunityToFile(opportunityBet);
                    }
                }
            }
        }
        return emptyList();
    }

    private void writeSimOrders(OpportunityBet opportunityBet) {
        log.info("Writing simulation orders to BigQuery Db");
        Map<String, List<PlaceInstruction>> ordersPerMarket = prepareOrdersPerMarket(opportunityBet);
        Instant now = Instant.now();
        List<List<String>> listOfOrders = ordersPerMarket.entrySet().stream().map(entry -> entry.getValue().stream().map(placeInstruction -> {
            SimOrder simOrder = new SimOrder();
            simOrder.setNode(appProperties.getNode());
            simOrder.setOrderTimeStamp(now);
            simOrder.setOrderId(UUID.randomUUID().toString().replace("-", ""));
            simOrder.setEventId(opportunityBet.getEvent().getId());
            simOrder.setMarketId(entry.getKey());
            simOrder.setOpportunityId(opportunityBet.getOpportunityId());
            Optional<OpportunitySelection> opportunitySelection = opportunityBet.getSelections().stream()
                    .filter(selection -> selection.getMarketSelection().getMarketId().equals(entry.getKey())
                            && selection.getMarketSelection().getRunnerCatalog().getSelectionId().equals(placeInstruction.getSelectionId()))
                    .findFirst();
            if (opportunitySelection.isPresent()) {
                Side side = opportunitySelection.get().getMarketSelection().getUserRunnerCode().getSide();
                simOrder.setOrderSide(side.toString());
                simOrder.setBookRunner(opportunitySelection.get().getMarketSelection().getUserRunnerCode().getCode());
                if (Side.BACK == side) {
                    simOrder.setOrderAllocation(opportunitySelection.get().getSelectionPrice().getBack().getSize());
                    simOrder.setOrderPrice(opportunitySelection.get().getSelectionPrice().getBack().getPrice());
                } else {
                    simOrder.setOrderAllocation(opportunitySelection.get().getSelectionPrice().getLay().getSize());
                    simOrder.setOrderPrice(opportunitySelection.get().getSelectionPrice().getLay().getPrice());
                }
            }
            simOrder.setOrderType(placeInstruction.getOrderType().toString());
            simOrder.setSelectionId(placeInstruction.getSelectionId());
            simOrder.setVenue("Betfair");
            simOrder.setStrategyId(opportunityBet.getStrategyId());
            simOrder.setEventName(opportunityBet.getEvent().getName());
            simOrder.setOrderAllocationCurrency(accountProperties.getCurrencyCode());
            simOrder.setInPlay(opportunityBet.getInPlay());
            return simOrder.toCsvData();
        }).collect(Collectors.toList())).collect(Collectors.toList());
        List<String> simOrders = listOfOrders.stream().flatMap(Collection::stream).collect(Collectors.toList());
        bigQueryService.insertRows(RESEARCH, "sim_orders", simOrders);
    }

    private void writeOpportunityToFile(OpportunityBet opportunityBet) {
        log.info("Writing opportunity to file");
        try {
            String json = mapper.writeValueAsString(opportunityBet);
            Path path = Paths.get(String.format("%s/strategy/opportunities/%s/%s/%s.json", appProperties.getDataDirectory(), LocalDate.now().format(DATE_FORMATTER), opportunityBet.getStrategyId(), UUID.randomUUID()));
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            Files.write(path, singletonList(json), StandardCharsets.UTF_8);
            log.info("opportunityBet={}", json);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error writing strategy trade...%s", opportunityBet), e);
        }
    }

    private List<PlaceInstructionReport> placePerMarketOrdersAndCollectResponse(OpportunityBet opportunityWithAllocations) {
        log.info("Placing live bet orders");
        List<PlaceInstructionReport> reports = new ArrayList<>();
        Map<String, List<PlaceInstruction>> ordersPerMarket = prepareOrdersPerMarket(opportunityWithAllocations);
        List<CompletableFuture<Optional<List<PlaceInstructionReport>>>> orderReports = ordersPerMarket.entrySet()
                .stream()
                .map(entry -> placeOrderPerMarket(entry.getKey(), entry.getValue(), opportunityWithAllocations))
                .collect(Collectors.toList());
        CompletableFuture<Void> combinedResult = CompletableFuture.allOf(orderReports.toArray(CompletableFuture[]::new));
        try {
            combinedResult.get();
            for (CompletableFuture<Optional<List<PlaceInstructionReport>>> orderReportFuture : orderReports) {
                if (orderReportFuture.get().isPresent()) {
                    reports.addAll(orderReportFuture.get().get());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Interrupted while waiting for placed order for %s", opportunityWithAllocations));
        } catch (ExecutionException e) {
            log.error("Please investigate error raised from placing orders for {}", opportunityWithAllocations, e);
        }
        return reports;
    }

    private CompletableFuture<Optional<List<PlaceInstructionReport>>> placeOrderPerMarket(String marketId, List<PlaceInstruction> placeInstructions, OpportunityBet opportunityBet) {
        log.info("orderRequest marketId={} instructions={}", marketId, placeInstructions);
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return CompletableFuture.supplyAsync(() -> {
            try {
                MDC.clear();
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                BetfairServerResponse<PlaceExecutionReport> serverResponse = betfairReferenceClient.placeOrders(marketId, placeInstructions, null);
                if (serverResponse != null) {
                    log.info("Status={} report={}", serverResponse.getResponse().getStatus(), serverResponse.getResponse());
                    bigQueryService.insertRows(PULSE_REPORTING, "orders_bets", betReportCsvData(serverResponse.getResponse(), opportunityBet));
                    if (serverResponse.getResponse().getStatus() != ExecutionReportStatus.SUCCESS) {
                        log.info("Sending email request async for failed orders");
                        try {
                            String json = mapper.writeValueAsString(serverResponse.getResponse());
                            emailNotifier.sendMessageAsync(json, "Pulse: OrderBet Error, please investigate", emailProperties.getTo());
                        } catch (JsonProcessingException e) {
                            log.error("Error converting placeExecutionReport={} to json", serverResponse.getResponse());
                        }
                    }
                    return Optional.of(serverResponse.getResponse().getInstructionReports());
                } else {
                    log.error("OrderFailure for marketId={} instructions={}", marketId, placeInstructions);
                    return Optional.empty();
                }
            } catch (RuntimeException rte) {
                log.error("OrderFailure for marketId={} instructions={}", marketId, placeInstructions);
                return Optional.empty();
            } finally {
                MDC.clear();
            }
        }, worker);
    }

    private List<String> betReportCsvData(PlaceExecutionReport response, OpportunityBet opportunityBet) {
        return response.getInstructionReports().stream()
                .map(instructionReport -> toCsvData(response, instructionReport, opportunityBet))
                .collect(Collectors.toList());
    }

    private String toCsvData(PlaceExecutionReport executionReport, PlaceInstructionReport instructionReport, OpportunityBet opportunityBet) {
        OrderReport orderReport = new OrderReport();
        orderReport.setNode(appProperties.getNode());
        if (executionReport.getStatus() != ExecutionReportStatus.SUCCESS) {
            orderReport.setAbortReason(instructionReport.getErrorCode().toString());
        }
        orderReport.setOrderStatus(instructionReport.getStatus().toString());
        orderReport.setBetAmount(instructionReport.getSizeMatched());
        orderReport.setBetId(instructionReport.getBetId());
        orderReport.setBetPrice(instructionReport.getAveragePriceMatched());
        orderReport.setEventId(opportunityBet.getEvent().getId());
        orderReport.setMarketId(executionReport.getMarketId());
        orderReport.setOpportunityId(opportunityBet.getOpportunityId());
        Optional<OpportunitySelection> opportunitySelection = opportunityBet.getSelections().stream()
                .filter(selection -> selection.getMarketSelection().getMarketId().equals(executionReport.getMarketId())
                        && selection.getMarketSelection().getRunnerCatalog().getSelectionId().equals(instructionReport.getInstruction().getSelectionId()))
                .findFirst();
        if (opportunitySelection.isPresent()) {
            Side side = opportunitySelection.get().getMarketSelection().getUserRunnerCode().getSide();
            orderReport.setOrderSide(side.toString());
            orderReport.setBookRunner(opportunitySelection.get().getMarketSelection().getUserRunnerCode().getCode());
            if (Side.BACK == side) {
                orderReport.setOrderAllocation(opportunitySelection.get().getSelectionPrice().getBack().getSize());
                orderReport.setOrderPrice(opportunitySelection.get().getSelectionPrice().getBack().getPrice());
            } else {
                orderReport.setOrderAllocation(opportunitySelection.get().getSelectionPrice().getLay().getSize());
                orderReport.setOrderPrice(opportunitySelection.get().getSelectionPrice().getLay().getPrice());
            }
        }
        orderReport.setOrderType(instructionReport.getInstruction().getOrderType().toString());
        orderReport.setOrderTimeStamp(instructionReport.getPlacedDate());
        orderReport.setSelectionId(instructionReport.getInstruction().getSelectionId());
        orderReport.setVenue("Betfair");
        orderReport.setExecutionStatus(executionReport.getStatus().toString());
        orderReport.setStrategyId(opportunityBet.getStrategyId());
        orderReport.setEventName(opportunityBet.getEvent().getName());
        orderReport.setOrderAllocationCurrency(accountProperties.getCurrencyCode());
        orderReport.setBetAmountCurrency(accountProperties.getCurrencyCode());
        orderReport.setInPlay(opportunityBet.getInPlay());
        return orderReport.toCsvData();
    }

    private Map<String, List<PlaceInstruction>> prepareOrdersPerMarket(OpportunityBet opportunityWithAllocations) {
        Map<String, List<PlaceInstruction>> orders = new HashMap<>();
        opportunityWithAllocations.getSelections().forEach(opportunitySelection -> {
            String marketId = opportunitySelection.getMarketSelection().getMarketId();
            orders.putIfAbsent(marketId, new ArrayList<>());
            Side side = opportunitySelection.getMarketSelection().getUserRunnerCode().getSide();
            double size;
            double price;
            if (Side.BACK == side) {
                size = opportunitySelection.getSelectionPrice().getBack().getSize();
                price = DEFAULT_BACK_LIMIT_PRICE;
            } else {
                size = opportunitySelection.getSelectionPrice().getLay().getSize();
                price = getLayLimitPrice(opportunitySelection.getSelectionPrice().getLay().getPrice());
            }
            LimitOrder limitOrder = new LimitOrder(size, price, PersistenceType.LAPSE);
            Long selectionId = opportunitySelection.getMarketSelection().getRunnerCatalog().getSelectionId();
            PlaceInstruction placeInstruction = new PlaceInstruction(OrderType.LIMIT, selectionId, 0, side, limitOrder, null, null);
            orders.get(marketId).add(placeInstruction);
        });
        return orders;
    }

    private OpportunityBet opportunityWithAllocations(OpportunityBet opportunityBet, List<Double> allocations) {
        List<OpportunitySelection> selectionWithAllocations = new ArrayList<>();
        for (int i = 0; i < opportunityBet.getSelections().size(); i++) {
            SelectionPrice selectionPrice = selectionPrice(opportunityBet.getSelections().get(i), allocations.get(i));
            selectionWithAllocations.add(OpportunitySelection.of(opportunityBet.getSelections().get(i).getMarketSelection(), selectionPrice));
        }
        return new OpportunityBet(
                opportunityBet.getEvent(),
                opportunityBet.getStrategyId(),
                opportunityBet.getTimeStamp(),
                selectionWithAllocations,
                opportunityBet.getAllocatorId(), opportunityBet.getOpportunityId(), opportunityBet.getInPlay());
    }

    private SelectionPrice selectionPrice(OpportunitySelection opportunitySelection, Double allocationSize) {
        Side side = opportunitySelection.getMarketSelection().getUserRunnerCode().getSide();
        if (Side.LAY == side) {
            PriceSize priceSize = new PriceSize(opportunitySelection.getSelectionPrice().getLay().getPrice(), allocationSize);
            return SelectionPrice.lay(priceSize);
        } else {
            PriceSize priceSize = new PriceSize(opportunitySelection.getSelectionPrice().getBack().getPrice(), allocationSize);
            return SelectionPrice.back(priceSize);
        }
    }

    private boolean isSafetyCheckSuccessful(OpportunityBet opportunityWithAllocations) {
        Optional<Strategy> strategy = strategyCache.getStrategyById(opportunityWithAllocations.getStrategyId());
        if (strategy.isEmpty()) {
            log.warn("SafetyCheck failure missing strategy id={} in strategyCache", opportunityWithAllocations.getStrategyId());
            return false;
        }
        StrategySpec strategySpec = strategy.get().getStrategySpec();
        Set<String> criteriaScores = strategySpec.getEventCriteria().getCurrentLiveScores();

        Integer eventId = Integer.valueOf(opportunityWithAllocations.getEvent().getId());
        if (strategySpec.getEventCriteria().isLive() && betfairInPlayService.isInPlay(eventId) && !criteriaScores.isEmpty()) {
            Optional<MatchScore> score = betfairInPlayService.score(eventId);
            if (score.isPresent()) {
                String currentScore = score.get().currentScore();
                if (!criteriaScores.contains(currentScore)) {
                    log.warn("SafetyCheck failure currentScore={} criteria={}", currentScore, criteriaScores);
                    return false;
                }
            }
        }

        for (OpportunitySelection opportunitySelection : opportunityWithAllocations.getSelections()) {
            Long selectionId = opportunitySelection.getMarketSelection().getRunnerCatalog().getSelectionId();
            Optional<MarketSnap> marketSnap = marketSnaps.getMarketSnap(opportunitySelection.getMarketSelection().getMarketId());
            if (marketSnap.isEmpty()) {
                log.warn("SafetyCheck failure missing marketSnap marketId={}", opportunitySelection.getMarketSelection().getMarketId());
                return false;
            }
            MarketDefinition marketDefinition = marketSnap.get().getMarketDefinition();
            if (MarketDefinition.StatusEnum.OPEN != marketDefinition.getStatus()) {
                log.warn("SafetyCheck failure marketStatus={}", marketDefinition.getStatus());
                return false;
            }
            Optional<RunnerDefinition> runnerDefOpt = marketDefinition.getRunners()
                    .stream()
                    .filter(runnerDefinition -> runnerDefinition.getId().equals(selectionId))
                    .findFirst();
            if (runnerDefOpt.isEmpty()) {
                log.warn("SafetyCheck failure missing runner definition for selectionId={}", selectionId);
                return false;
            }
            if (ACTIVE != runnerDefOpt.get().getStatus()) {
                log.warn("SafetyCheck failure runnerStatus={}", runnerDefOpt.get().getStatus());
                return false;
            }
            Optional<MarketRunnerSnap> marketRunnerSnap = marketSnap.get().getMarketRunners()
                    .stream()
                    .filter(runnerSnap -> runnerSnap.getRunnerId().getSelectionId() == selectionId)
                    .findFirst();
            if (marketRunnerSnap.isEmpty()) {
                log.warn("SafetyCheck failure missing runner marketSnap for selectionId={}", selectionId);
                return false;
            }
            MarketRunnerPrices prices = marketRunnerSnap.get().getPrices();
            LevelPriceSize backPrice = prices.getBdatb().get(0);
            LevelPriceSize layPrice = prices.getBdatl().get(0);
            if (Side.BACK == opportunitySelection.getMarketSelection().getUserRunnerCode().getSide()) {
                if (backPrice.getPrice() < opportunitySelection.getSelectionPrice().getBack().getPrice()) {
                    log.warn("SafetyCheck failure backPrice={} less than selectionPrice={} for runnerCode={}",
                            backPrice.getPrice(),
                            opportunitySelection.getSelectionPrice().getBack().getPrice(),
                            opportunitySelection.getMarketSelection().getUserRunnerCode().getCode());
                    return false;
                }
                if (opportunitySelection.getSelectionPrice().getBack().getSize() > backPrice.getSize()) {
                    log.warn("SafetyCheck failure backSize={} less than selectionSize={} for runnerCode={}",
                            backPrice.getSize(),
                            opportunitySelection.getSelectionPrice().getBack().getSize(),
                            opportunitySelection.getMarketSelection().getUserRunnerCode().getCode());
                    return false;
                }
            } else {
                if (layPrice.getPrice() > opportunitySelection.getSelectionPrice().getLay().getPrice()) {
                    log.warn("SafetyCheck failure layPrice={} less than selectionPrice={} for runnerCode={}",
                            layPrice.getPrice(),
                            opportunitySelection.getSelectionPrice().getLay().getPrice(),
                            opportunitySelection.getMarketSelection().getUserRunnerCode().getCode());
                    return false;
                }
                if (opportunitySelection.getSelectionPrice().getLay().getSize() > layPrice.getSize()) {
                    log.warn("SafetyCheck failure laySize={} less than selectionSize={} for runnerCode={}",
                            layPrice.getSize(),
                            opportunitySelection.getSelectionPrice().getLay().getSize(),
                            opportunitySelection.getMarketSelection().getUserRunnerCode().getCode());
                    return false;
                }
            }
        }
        return true;
    }

    @PreDestroy
    public void shutDown() {
        if (worker != null) {
            worker.shutdown();
        }
    }
}
