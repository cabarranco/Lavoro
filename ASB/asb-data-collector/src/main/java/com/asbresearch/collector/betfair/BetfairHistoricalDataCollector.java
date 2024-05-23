package com.asbresearch.collector.betfair;

import com.asbresearch.betfair.esa.cache.market.MarketRunnerPrices;
import com.asbresearch.betfair.esa.cache.market.MarketRunnerSnap;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.MarketSnaps;
import com.asbresearch.collector.betfair.model.BetfairHistoricalRecord;
import com.asbresearch.collector.betfair.model.RunnerPrice;
import com.asbresearch.common.bigquery.BigQueryService;
import com.betfair.esa.swagger.model.MarketDefinition;
import com.betfair.esa.swagger.model.MarketDefinition.StatusEnum;
import com.betfair.esa.swagger.model.RunnerDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.BigQueryUtil.HISTORICAL_DATA_TABLE;

@Component("BetfairHistoricalDataCollector")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "historicalDataCollector", havingValue = "on")
@DependsOn({"BetfairEsaSubscription"})
public class BetfairHistoricalDataCollector {
    private final MarketSnaps marketSnaps;
    private final Map<String, RunnerPrice> runnerPriceCache = new ConcurrentHashMap<>();
    private final BigQueryService bigQueryService;
    private final List<Integer> validHandicapPriority = List.of(21, 22, 29, 30, 37, 38, 45, 46);

    @Autowired
    public BetfairHistoricalDataCollector(MarketSnaps marketSnaps, BigQueryService bigQueryService) {
        this.marketSnaps = marketSnaps;
        this.bigQueryService = bigQueryService;
    }

    @Scheduled(cron = "${collector.historicalDataCollector.cronExpression:*/30 * * * * *}")
    public void scheduledPriceSnapshot() {
        log.debug("Begin scheduled HistoricalDataCollector");
        try {
            List<List<String>> ticksPerMarket = marketSnaps.getMarketIds().stream().map(this::selectionTicks).collect(Collectors.toList());
            try {
                ticksPerMarket.forEach(ticks -> bigQueryService.insertRows(BETSTORE_DATASET, HISTORICAL_DATA_TABLE, ticks));
            } catch (RuntimeException ex) {
                log.error("Error when historical tick data insert into big query", ex);
            }
        } finally {
            log.debug("End scheduled HistoricalDataCollector");
        }
    }

    private List<String> selectionTicks(String marketId) {
        log.debug("marketId={}", marketId);
        List<String> ticks = new ArrayList<>();
        try {
            Optional<MarketSnap> marketSnapOpt = marketSnaps.getMarketSnap(marketId);
            if (marketSnapOpt.isPresent()) {
                MarketSnap marketSnap = marketSnapOpt.get();
                MarketDefinition marketDefinition = marketSnap.getMarketDefinition();
                String eventId = marketDefinition.getEventId();
                double tradedVolume = marketSnap.getTradedVolume();
                StatusEnum status = marketDefinition.getStatus();
                Boolean inPlay = marketDefinition.getInPlay();
                List<MarketRunnerSnap> marketRunners = marketSnap.getMarketRunners();

                for (MarketRunnerSnap marketRunnerSnap : marketRunners) {
                    if ("ASIAN_HANDICAP".equals(marketDefinition.getMarketType())) {
                        if (!validHandicapPriority.contains(marketRunnerSnap.getDefinition().getSortPriority())) {
                            continue;
                        }
                    }
                    long selectionId = marketRunnerSnap.getRunnerId().getSelectionId();
                    MarketRunnerPrices prices = marketRunnerSnap.getPrices();
                    List<LevelPriceSize> bdatb = prices.getBdatb();
                    List<LevelPriceSize> bdatl = prices.getBdatl();
                    if (CollectionUtils.isEmpty(bdatb) || CollectionUtils.isEmpty(bdatl)) {
                        continue;
                    }
                    double backPrice = bdatb.get(0).getPrice();
                    double backSize = bdatb.get(0).getSize();
                    double layPrice = bdatl.get(0).getPrice();
                    double laySize = bdatl.get(0).getSize();
                    String asbSelection = asbSelection(marketDefinition.getMarketType(), marketRunnerSnap.getDefinition());
                    if (asbSelection != null) {
                        RunnerPrice currentPrice = RunnerPrice.of(backPrice, backSize, layPrice, laySize);
                        RunnerPrice previousPrice = runnerPriceCache.put(getSelectionCacheId(marketId, asbSelection), currentPrice);
                        if (previousPrice != null) {
                            if (!currentPrice.equals(previousPrice)) {
                                ticks.add(createRow(eventId, marketId, selectionId, asbSelection, status, inPlay, tradedVolume, currentPrice, marketSnap.getPublishTime()));
                            }
                        } else {
                            ticks.add(createRow(eventId, marketId, selectionId, asbSelection, status, inPlay, tradedVolume, currentPrice, marketSnap.getPublishTime()));
                        }
                    }
                }
            }
            log.debug("marketId={} {} ticks", marketId, ticks.size());
        } catch (RuntimeException ex) {
            log.error("Error extracting selection ticks for marketId={}", marketId, ex);
        }
        return ticks;
    }

    private String getSelectionCacheId(String marketId, String asbSelection) {
        return String.format("%s_%s", marketId, asbSelection);
    }

    private String asbSelection(String marketType, RunnerDefinition runnerDefinition) {
        switch (marketType) {
            case "MATCH_ODDS":
            case "MATCH_ODDS_UNMANAGED":
                return String.valueOf(runnerDefinition.getSortPriority());
            case "OVER_UNDER_05":
            case "OVER_UNDER_05_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 1:
                        return "2052";
                    case 2:
                        return "2051";
                }
            case "OVER_UNDER_15":
            case "OVER_UNDER_15_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 1:
                        return "2152";
                    case 2:
                        return "2151";
                }
            case "OVER_UNDER_25":
            case "OVER_UNDER_25_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 1:
                        return "2252";
                    case 2:
                        return "2251";
                }
            case "OVER_UNDER_35":
            case "OVER_UNDER_35_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 1:
                        return "2352";
                    case 2:
                        return "2351";
                }
            case "CORRECT_SCORE":
            case "CORRECT_SCORE_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 1:
                        return "300";
                    case 2:
                        return "301";
                    case 3:
                        return "302";
                    case 4:
                        return "303";
                    case 5:
                        return "310";
                    case 6:
                        return "311";
                    case 7:
                        return "312";
                    case 8:
                        return "313";
                    case 9:
                        return "320";
                    case 10:
                        return "321";
                    case 11:
                        return "322";
                    case 12:
                        return "323";
                    case 13:
                        return "330";
                    case 14:
                        return "331";
                    case 15:
                        return "332";
                    case 16:
                        return "333";
                    case 17:
                        return "340";
                    case 18:
                        return "304";
                    case 19:
                        return "344";
                }
            case "ASIAN_HANDICAP":
            case "ASIAN_HANDICAP_UNMANAGED":
                switch (runnerDefinition.getSortPriority()) {
                    case 21:
                        return "41215";
                    case 22:
                        return "42115";
                    case 29:
                        return "41205";
                    case 30:
                        return "42105";
                    case 37:
                        return "41105";
                    case 38:
                        return "42205";
                    case 45:
                        return "41115";
                    case 46:
                        return "42215";
                }
        }
        return null;
    }

    private String createRow(String eventId,
                             String marketId,
                             long selectionId,
                             String asbSelection,
                             StatusEnum status,
                             Boolean inPlay,
                             double tradedVolume,
                             RunnerPrice runnerPrice,
                             Instant publishTime) {
        BetfairHistoricalRecord record = BetfairHistoricalRecord.builder()
                .eventId(eventId)
                .marketId(marketId)
                .asbSelectionId(asbSelection)
                .selectionId(selectionId)
                .status(status)
                .inplay(inPlay)
                .totalMatched(tradedVolume)
                .runnerPrice(runnerPrice)
                .publishTime(publishTime)
                .build();
        return record.toString();
    }
}
