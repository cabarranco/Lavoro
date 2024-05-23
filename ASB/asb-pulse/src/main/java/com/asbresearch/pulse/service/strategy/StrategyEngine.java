package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.ClientCache;
import com.asbresearch.betfair.esa.auth.InvalidCredentialException;
import com.asbresearch.betfair.esa.cache.market.MarketChangeEvent;
import com.asbresearch.betfair.esa.protocol.ConnectionException;
import com.asbresearch.betfair.esa.protocol.StatusException;
import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.inplay.model.InPlayRequest;
import com.asbresearch.pulse.config.BetfairClientsConfig;
import com.asbresearch.pulse.config.EsaProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.service.MarketSnaps;
import com.asbresearch.pulse.service.OpportunityQueue;
import com.asbresearch.pulse.util.ThreadUtils;
import com.betfair.esa.swagger.model.MarketDataFilter;
import com.betfair.esa.swagger.model.MarketFilter;
import com.betfair.esa.swagger.model.MarketSubscriptionMessage;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.betfair.esa.swagger.model.MarketDataFilter.FieldsEnum.EX_BEST_OFFERS_DISP;
import static com.betfair.esa.swagger.model.MarketDataFilter.FieldsEnum.EX_MARKET_DEF;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

@DependsOn({"plmEngine"})
@EnableConfigurationProperties({EsaProperties.class, StrategyProperties.class})
@Service
@Slf4j
public class StrategyEngine {
    private final static int MAX_TASK_QUEUE_SIZE = 500;

    private final StrategyCache strategies;
    private List<Client> clients;
    private final Map<String, List<Strategy>> marketsPerStrategy = new ConcurrentHashMap<>();
    private final Map<String, Instant> timestampPerMarketChange = new ConcurrentHashMap<>();
    private final BetfairClientsConfig betfairClientsConfig;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final OpportunityQueue opportunityQueue;
    private final EsaProperties esaProperties;
    private final MarketSnaps marketSnaps;
    private final LIFOStrategyTaskQueue strategyTaskQueue = new LIFOStrategyTaskQueue();
    private final StrategyEventIgnoreContainer strategyEventIgnoreContainer;
    private final BetfairInPlayService betfairInPlayService;

    @Autowired
    public StrategyEngine(MarketSnaps marketSnaps,
                          StrategyCache strategies,
                          OpportunityQueue opportunityQueue,
                          StrategyProperties strategyProperties,
                          BetfairClientsConfig betfairClientsConfig,
                          EsaProperties esaProperties,
                          StrategyEventIgnoreContainer strategyEventIgnoreContainer,
                          BetfairInPlayService betfairInPlayService) {

        checkNotNull(marketSnaps, "marketSnaps must be provided");
        checkNotNull(strategies, "strategyCache must be provided");
        checkNotNull(opportunityQueue, "opportunityQueue must be provided");
        checkNotNull(strategyProperties, "strategyProperties must be provided");
        checkNotNull(betfairClientsConfig, "betfairClientsConfig must be provided");
        checkNotNull(esaProperties, "esaProperties must be provided");
        checkNotNull(betfairInPlayService, "betfairInPlayService must be provided");

        this.threadPoolExecutor = createThreadPoolExecutor(strategyProperties);
        this.strategies = strategies;
        this.betfairClientsConfig = betfairClientsConfig;
        this.opportunityQueue = opportunityQueue;
        this.esaProperties = esaProperties;
        this.marketSnaps = marketSnaps;
        this.strategyEventIgnoreContainer = strategyEventIgnoreContainer;
        this.betfairInPlayService = betfairInPlayService;
    }

    private ThreadPoolExecutor createThreadPoolExecutor(StrategyProperties strategyProperties) {
        ThreadFactory threadFactory = ThreadUtils.threadFactoryBuilder("se").build();
        int nThreads = strategyProperties.getThreads();
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, strategyTaskQueue, threadFactory);
    }

    @PostConstruct
    public void start() {
        requestInPlayService();
        subscribeToMarketData();
    }

    private void subscribeToMarketData() {
        try {
            strategies.getCache().values().forEach(strategy -> {
                strategy.marketSnapsAndOpportunityQueue(marketSnaps, opportunityQueue);
                Set<String> marketSubscriptions = strategy.marketSubscriptions();
                marketSubscriptions.forEach(marketId -> {
                    marketsPerStrategy.putIfAbsent(marketId, new CopyOnWriteArrayList());
                    marketsPerStrategy.get(marketId).add(strategy);
                });
            });
            log.info("Subscribing {} markets: {}", marketsPerStrategy.keySet().size(), marketsPerStrategy.keySet());
            List<Client> clients = new ArrayList<>();
            if (!marketsPerStrategy.isEmpty()) {
                Iterable<List<String>> marketIdBatches = partition(marketsPerStrategy.keySet());
                log.info("Connecting to betfair with {} batches", Iterables.size(marketIdBatches));
                for (List<String> marketIds : marketIdBatches) {
                    Client client = betfairClientsConfig.createEsaClient();
                    ClientCache clientCache = new ClientCache(client);
                    marketIds.forEach(marketId -> marketSnaps.put(marketId, clientCache.getMarketCache()));
                    clientCache.getMarketCache().addMarketChangeListener(marketChangeEvent -> updateAndTriggerStrategies(marketChangeEvent));
                    log.info("Connecting to betfair {} markets: {}", marketIds.size(), marketIds);
                    clientCache.subscribeMarkets(subscriptions(marketIds));
                    clients.add(client);
                }
            }
            this.clients = clients;
            log.info("StrategyEngine started with {} esa clients", clients.size());
        } catch (ConnectionException | InvalidCredentialException | StatusException e) {
            throw new RuntimeException("Error starting Strategy executor", e);
        }
    }

    private void requestInPlayService() {
        Instant now = Instant.now();
        Set<InPlayRequest> inPlayRequests = strategies.getCache().values()
                .stream()
                .filter(strategy -> strategy.getStrategySpec().getEventCriteria().isLive())
                .map(strategy -> strategy.events().stream().map(event -> InPlayRequest.of(Integer.valueOf(event.getId()), event.getOpenDate())).collect(toSet()))
                .flatMap(requests -> requests.stream())
                .filter(inPlayRequest -> inPlayRequest.getStartTime().isAfter(now))
                .collect(Collectors.toSet());
        betfairInPlayService.requestPolling(inPlayRequests);
    }

    private MarketSubscriptionMessage subscriptions(List<String> marketIds) {
        MarketFilter marketFilter = new MarketFilter();
        marketFilter.setMarketIds(marketIds);
        MarketSubscriptionMessage subscription = new MarketSubscriptionMessage();
        subscription.setMarketFilter(marketFilter);
        subscription.setMarketDataFilter(marketDataFilter());
        return subscription;
    }

    protected Iterable<List<String>> partition(Set<String> marketIds) {
        int remainder = marketIds.size() % esaProperties.getMaxConnections();
        int size = marketIds.size() / esaProperties.getMaxConnections();
        return Iterables.partition(marketIds, remainder == 0 ? size : size + 1);
    }

    private void updateAndTriggerStrategies(MarketChangeEvent marketChangeEvent) {
        log.debug("marketChangeEvent->{}", marketChangeEvent);
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        List<Strategy> strategies = marketsPerStrategy.getOrDefault(marketChangeEvent.getMarket().getMarketId(), Collections.emptyList());
        Instant now = Instant.now();
        timestampPerMarketChange.put(marketChangeEvent.getMarket().getMarketId(), now);
        int queueSize = threadPoolExecutor.getQueue().size();
        log.info("{} pending market change tasks", queueSize);
        if (queueSize >= MAX_TASK_QUEUE_SIZE) {
            strategyTaskQueue.removeLast();
        }
        strategies.forEach(strategy -> {
            if (!strategyEventIgnoreContainer.contains(strategy.getId(), marketChangeEvent.getMarket().getSnap().getMarketDefinition().getEventId())) {
                threadPoolExecutor.submit(new StrategyMarketChangeRunner(marketChangeEvent, copyOfContextMap, strategy, now, timestampPerMarketChange));
            }
        });
    }

    public synchronized void stop() {
        if (clients != null) {
            clients.forEach(client -> client.stop());
        }
        marketsPerStrategy.clear();
        marketSnaps.clear();
        log.info("StrategyEngine stopped");
    }

    protected MarketDataFilter marketDataFilter() {
        MarketDataFilter filter = new MarketDataFilter();
        filter.setFields(Arrays.asList(EX_BEST_OFFERS_DISP, EX_MARKET_DEF));
        return filter;
    }

    public synchronized void restart() {
        stop();
        start();
    }

    @PreDestroy
    public void shutDown() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
        }
    }
}
