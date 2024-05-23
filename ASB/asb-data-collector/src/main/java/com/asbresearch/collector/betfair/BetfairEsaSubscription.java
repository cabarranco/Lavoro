package com.asbresearch.collector.betfair;

import com.asbresearch.betfair.esa.ClientCache;
import com.asbresearch.betfair.esa.auth.InvalidCredentialException;
import com.asbresearch.betfair.esa.cache.market.Market;
import com.asbresearch.betfair.esa.cache.market.MarketCache;
import com.asbresearch.betfair.esa.cache.market.MarketChangeEvent;
import com.asbresearch.betfair.esa.cache.util.MarketSnaps;
import com.asbresearch.betfair.esa.protocol.ConnectionException;
import com.asbresearch.betfair.esa.protocol.StatusException;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.collector.config.BetfairClientsConfig;
import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.EsaProperties;
import com.betfair.esa.swagger.model.MarketDataFilter;
import com.betfair.esa.swagger.model.MarketFilter;
import com.betfair.esa.swagger.model.MarketSubscriptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.betfair.esa.swagger.model.MarketDataFilter.FieldsEnum.*;
import static org.apache.commons.collections4.CollectionUtils.subtract;

@EnableConfigurationProperties({EsaProperties.class, CollectorProperties.class})
@Service("BetfairEsaSubscription")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "betfairEsaSubscription", havingValue = "on")
@DependsOn({"BetfairReferenceClient"})
public class BetfairEsaSubscription {
    private final BigQueryService bigQueryService;
    private final BetfairClientsConfig betfairClientsConfig;
    private final EsaProperties esaProperties;
    private final MarketSnaps marketSnaps;
    private final EventsOfTheDayProvider eventsOfTheDayProvider;
    private final Map<String, ClientCache> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> marketToClients = new ConcurrentHashMap<>();
    private final Map<String, Boolean> closedMarkets = new ConcurrentHashMap<>();

    @Autowired
    public BetfairEsaSubscription(BigQueryService bigQueryService,
                                  BetfairClientsConfig betfairClientsConfig,
                                  EsaProperties esaProperties,
                                  MarketSnaps marketSnaps,
                                  EventsOfTheDayProvider eventsOfTheDayProvider) {
        this.bigQueryService = bigQueryService;
        this.esaProperties = esaProperties;
        this.betfairClientsConfig = betfairClientsConfig;
        this.marketSnaps = marketSnaps;
        this.eventsOfTheDayProvider = eventsOfTheDayProvider;
    }

    @PostConstruct
    public void subscribeAtStartUp() {
        String query = "SELECT distinct marketId FROM `betstore.betfair_market_catalogue` WHERE date(startTime) >= current_date()";
        try {
            log.info("sql={}", query);
            List<String> dbMarketIds = bigQueryService.performQuery(query).stream()
                    .map(row -> row.get("marketId").get().toString())
                    .collect(Collectors.toList());
            doSubscribe(new LinkedList<>(dbMarketIds));
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error reading cached market ids from BQ sql={}", query, e);
        }
    }

    @Scheduled(fixedDelay = 120000)
    public void subscribe() {
        doSubscribe(new LinkedList<>(pendingMarketsForTheDay()));
    }

    private void doSubscribe(LinkedList<String> pendingMarketsForTheDay) {
        log.info("{} pending market subscriptions", pendingMarketsForTheDay.size());
        int clientCounter = clients.size();
        while (!pendingMarketsForTheDay.isEmpty() && clients.size() < esaProperties.getMaxConnections()) {
            List<String> perClientSubscriptions = getPerClientSubscriptions(pendingMarketsForTheDay, esaProperties.getMaxMarketIdsPerConn());
            String clientName = "client-" + clientCounter++;
            clientSubscriptions.putIfAbsent(clientName, new HashSet<>());
            log.debug("Subscribing to betfair {} {} markets", clientName, perClientSubscriptions.size());
            clients.putIfAbsent(clientName, new ClientCache(betfairClientsConfig.createEsaClient()));
            List<String> perClientSubscribed = subscribe(perClientSubscriptions, clientName);
            if (perClientSubscribed.isEmpty()) {
                pendingMarketsForTheDay.addAll(perClientSubscriptions);
            }
        }

        if (!pendingMarketsForTheDay.isEmpty()) {
            clients.forEach((clientName, clientCache) -> {
                Set<String> currentSubscriptions = clientSubscriptions.get(clientName);
                int availableSubscription = esaProperties.getMaxMarketIdsPerConn() - currentSubscriptions.size();
                List<String> perClientSubscriptions = getPerClientSubscriptions(pendingMarketsForTheDay, availableSubscription);
                perClientSubscriptions.addAll(currentSubscriptions);
                List<String> perClientSubscribed = subscribe(perClientSubscriptions, clientName);
                if (perClientSubscribed.isEmpty()) {
                    pendingMarketsForTheDay.addAll(perClientSubscriptions);
                }
            });
        }

        clientSubscriptions.forEach((clientName, subscriptions) -> log.info("Subscription Summary {} subscriptions={}", clientName, subscriptions.size()));
        if (!pendingMarketsForTheDay.isEmpty()) {
            log.error("Cannot Subscribe to betfair with {} markets, {}", pendingMarketsForTheDay.size(), pendingMarketsForTheDay);
        } else {
            log.info("Successfully subscribed all pending markets");
        }
    }

    private List<String> subscribe(List<String> perClientSubscriptions, String clientName) {
        List<String> subscribed = new ArrayList<>();
        try {
            ClientCache clientCache = clients.get(clientName);
            clientCache.getClient().stop();
            clientCache.subscribeMarkets(subscriptions(perClientSubscriptions));
            MarketCache marketCache = clientCache.getMarketCache();
            marketCache.addMarketChangeListener(this::updateRemovedMarket);
            perClientSubscriptions.forEach(marketId -> {
                marketSnaps.put(marketId, marketCache);
                marketToClients.put(marketId, clientName);
            });
            clientSubscriptions.get(clientName).addAll(perClientSubscriptions);
            subscribed.addAll(perClientSubscriptions);
        } catch (InvalidCredentialException | StatusException | ConnectionException e) {
            log.error("Error subscribing to betfair {}", clientName, e);
        }
        return subscribed;
    }

    private List<String> getPerClientSubscriptions(LinkedList<String> perClientPendingMarkets, int perClientRemainingSubscriptions) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < perClientRemainingSubscriptions; i++) {
            if (!perClientPendingMarkets.isEmpty()) {
                result.add(perClientPendingMarkets.remove());
            } else {
                break;
            }
        }
        return result;
    }

    private Collection<String> pendingMarketsForTheDay() {
        Set<String> currentMarketIds = eventsOfTheDayProvider.getMarketCatalogueOfTheDay()
                .stream()
                .map(MarketCatalogue::getMarketId)
                .collect(Collectors.toSet());
        return subtract(subtract(currentMarketIds, marketToClients.keySet()), closedMarkets.keySet());
    }

    @PreDestroy
    public void shutDown() {
        clients.values().forEach(clientCache -> clientCache.getClient().stop());
    }

    private void updateRemovedMarket(MarketChangeEvent marketChangeEvent) {
        log.debug("UpdateRemovedMarket marketChangeEvent->{}", marketChangeEvent);
        Market market = marketChangeEvent.getMarket();
        if (market.isClosed()) {
            String marketId = marketChangeEvent.getSnap().getMarketId();
            marketSnaps.remove(marketId);
            String clientName = marketToClients.get(marketId);
            clientSubscriptions.get(clientName).remove(marketId);
            closedMarkets.put(marketId, Boolean.TRUE);
            marketToClients.remove(marketId);
            log.info("Removed marketId={} from {}", marketId, clientName);
        }
    }

    private MarketSubscriptionMessage subscriptions(List<String> marketIds) {
        MarketFilter marketFilter = new MarketFilter();
        marketFilter.setMarketIds(marketIds);
        MarketSubscriptionMessage subscription = new MarketSubscriptionMessage();
        subscription.setMarketFilter(marketFilter);
        subscription.setMarketDataFilter(marketDataFilter());
        return subscription;
    }

    protected MarketDataFilter marketDataFilter() {
        MarketDataFilter filter = new MarketDataFilter();
        filter.setFields(Arrays.asList(EX_BEST_OFFERS_DISP, EX_MARKET_DEF, EX_TRADED_VOL));
        return filter;
    }
}