package com.asbresearch.metrics.metrics;

import com.asbresearch.metrics.metrics.facade.BetfairFacade;
import com.asbresearch.metrics.metrics.facade.BigQueryFacade;
import com.asbresearch.metrics.metrics.models.betfair.ClearedOrders;
import com.asbresearch.metrics.metrics.models.bigquery.BigQueryInsertLine;
import com.asbresearch.metrics.metrics.models.bigquery.DailyStrategiesMI;
import com.asbresearch.metrics.metrics.models.bigquery.DailySupervisoryMI;
import com.asbresearch.metrics.metrics.models.bigquery.Row;
import com.asbresearch.metrics.metrics.services.BigQueryService;
import com.asbresearch.metrics.metrics.services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
public class Bootstrapper {

    private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

    @Autowired
    private BetfairFacade betfairFacade;

    @Autowired
    private BigQueryService bigQueryService;

    @Autowired
    private BigQueryFacade bigQueryFacade;

    @Autowired
    private MailService mailService;

    @PostConstruct
    private void init() {
        log.info("Bootstrapper initialization logic ...");

        DailySupervisoryMI dailySupervisoryMI = new DailySupervisoryMI();

        ClearedOrders clearedOrders = betfairFacade.getListClearedOrders();
        List<String> strategiesIds = bigQueryFacade.getStrategiesIds();


        bigQueryFacade.calculateTradesWinningLosing(clearedOrders);

        double wonOrdersProfit = bigQueryFacade.getWonOrdersProfit();
        double lostOrdersProfit = bigQueryFacade.getLostOrdersProfit();
        Double todayBalance = bigQueryFacade.getTodayBalance();
        Double availableToBetYesterday = bigQueryFacade.getYesterdayBalance();
        Double sumBackBetAmount = bigQueryFacade.getSumBackBetAmount();
        Double sumLayBetAmount = bigQueryFacade.getSumLayBetAmount();
        int tradesNumbers = bigQueryFacade.getTradesNumbers();
        int tradesWinning = bigQueryFacade.getTradesWinning();
        int tradesLosing = bigQueryFacade.getTradesLosing();
        int failedOrders = bigQueryFacade.getFailedOrders();
        int fullyMatched = bigQueryFacade.getFullyMatched();
        int bestMatched = bigQueryFacade.getBestMatched();
        int worstMatched = bigQueryFacade.getWorstMatched();
        int partiallyMatched = bigQueryFacade.getPartiallyMatched();
        int inPlay = bigQueryFacade.getInPlay();
        int eventsAvailable = bigQueryFacade.getEventsAvailable();
        int eventsWithTrade = bigQueryFacade.getEventsWithTrade();

        Double investment = sumBackBetAmount + sumLayBetAmount;

        dailySupervisoryMI.setBalance(todayBalance);
        dailySupervisoryMI.setReturnOnCapital(availableToBetYesterday);
        dailySupervisoryMI.setRoi(availableToBetYesterday, investment);
        dailySupervisoryMI.setBur(availableToBetYesterday, investment);
        dailySupervisoryMI.setTotalNetProfit(availableToBetYesterday);
        dailySupervisoryMI.setTotalGrossProfit(clearedOrders.getTotalGrossProfit());
        dailySupervisoryMI.setGrossProfit(clearedOrders.getGrossProfit());
        dailySupervisoryMI.setGrossLoss(clearedOrders.getGrossLoss());
        dailySupervisoryMI.setGrossProfitFactor(dailySupervisoryMI.getGrossProfit() / dailySupervisoryMI.getGrossLoss());
        dailySupervisoryMI.setTradesNumber(tradesNumbers);
        dailySupervisoryMI.setTradesProfitableRate((float)tradesWinning / tradesNumbers);
        dailySupervisoryMI.setTradesWinning(tradesWinning);
        dailySupervisoryMI.setTradesLosing(tradesLosing);
        dailySupervisoryMI.setAverageTradeNetProfit(dailySupervisoryMI.getTotalNetProfit() / tradesNumbers);
        dailySupervisoryMI.setOrdersNumber(clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersFailedRate((float)failedOrders / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersNumber(clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersFullyMatchedRate((float)fullyMatched / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersBestMatchedRate((float)bestMatched / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersWorstMatchedRate((float)worstMatched / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersPartiallyMatchedRate((float)partiallyMatched / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setOrdersInPlayRate((float)inPlay / clearedOrders.getClearedOrders().size());
        dailySupervisoryMI.setEventsAvailable(eventsAvailable);
        dailySupervisoryMI.setEventsTradedRate((float)eventsWithTrade / eventsAvailable);
        dailySupervisoryMI.setLargestWinningTradeProfit(wonOrdersProfit);
        dailySupervisoryMI.setLargestLosingTradeLoss(lostOrdersProfit);

        
        bigQueryService.insertLine(
                "daily_supervisory_mi",
            new BigQueryInsertLine(
                    Collections.singletonList(new Row<>(dailySupervisoryMI))
            )
        );

        List<Row> dailyStrategiesMIList = new ArrayList<>();
        HashMap<String,List<DailyStrategiesMI>> dailyStrategiesMIMap = new HashMap<>();

        for (String strategyId : strategiesIds) {

            List<String> betIds = bigQueryFacade.getBetIds(strategyId);
            List<DailyStrategiesMI> list = new ArrayList<>();

            double grossProfit = clearedOrders.getGrossProfit(betIds);
            double grossLoss = clearedOrders.getGrossLoss(betIds);
            int strategyTradesNumbers = bigQueryFacade.getTradesNumbers(strategyId);

            double strategyWonOrdersProfit = bigQueryFacade.getWonOrdersProfit();
            double strategyLostOrdersProfit = bigQueryFacade.getLostOrdersProfit();

            bigQueryFacade.calculateTradesWinningLosing(strategyId, clearedOrders.filter(betIds));

            int strategyTradesWinning = bigQueryFacade.getTradesWinning();
            int strategyTradesLosing = bigQueryFacade.getTradesLosing();

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.LARGEST_LOSING_TRADE_LOSS.value(),
                            strategyId,
                            strategyLostOrdersProfit
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.LARGEST_LOSING_TRADE_LOSS.value(),
                                    strategyId,
                                    strategyLostOrdersProfit
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.LARGEST_WINNING_TRADE_PROFIT.value(),
                            strategyId,
                            strategyWonOrdersProfit
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.LARGEST_WINNING_TRADE_PROFIT.value(),
                                    strategyId,
                                    strategyWonOrdersProfit
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.TOTAL_GROSS_PROFIT.value(),
                            strategyId,
                            clearedOrders.getTotalGrossProfit(betIds)
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                        new DailyStrategiesMI(
                                DailyStrategiesMI.Metric.TOTAL_GROSS_PROFIT.value(),
                                strategyId,
                                clearedOrders.getTotalGrossProfit(betIds)
                        )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.GROSS_PROFIT.value(),
                            strategyId,
                            grossProfit
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                        new DailyStrategiesMI(
                                DailyStrategiesMI.Metric.GROSS_PROFIT.value(),
                                strategyId,
                                grossProfit
                        )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.GROSS_LOSS.value(),
                            strategyId,
                            grossLoss
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                        new DailyStrategiesMI(
                                DailyStrategiesMI.Metric.GROSS_LOSS.value(),
                                strategyId,
                                grossLoss
                        )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.GROSS_PROFIT_FACTOR.value(),
                            strategyId,
                            grossProfit / grossLoss
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.GROSS_PROFIT_FACTOR.value(),
                                    strategyId,
                                    grossProfit / grossLoss
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.TRADES_NUMBER.value(),
                            strategyId,
                            (double)strategyTradesNumbers
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.TRADES_NUMBER.value(),
                                    strategyId,
                                    (double)strategyTradesNumbers
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.TRADES_PROFITABLE_RATE.value(),
                            strategyId,
                            ((double)strategyTradesWinning / strategyTradesNumbers) * 100
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.TRADES_PROFITABLE_RATE.value(),
                                    strategyId,
                                    ((double)strategyTradesWinning / strategyTradesNumbers) * 100
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.TRADES_WINNING.value(),
                            strategyId,
                            (double)strategyTradesWinning
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.TRADES_WINNING.value(),
                                    strategyId,
                                    (double)strategyTradesWinning
                            )
                    )
            );

            list.add(
                    new DailyStrategiesMI(
                            DailyStrategiesMI.Metric.TRADES_LOSING.value(),
                            strategyId,
                            (double)strategyTradesLosing
                    )
            );
            dailyStrategiesMIList.add(
                    new Row<>(
                            new DailyStrategiesMI(
                                    DailyStrategiesMI.Metric.TRADES_LOSING.value(),
                                    strategyId,
                                    (double)strategyTradesLosing
                            )
                    )
            );

            dailyStrategiesMIMap.put(strategyId, list);
        }

        log.info(String.format("Writing on daily_strategies_mi %d rows", dailyStrategiesMIList.size()));

        if (dailyStrategiesMIList.size() > 0)
            bigQueryService.insertLine("daily_strategies_mi", new BigQueryInsertLine(dailyStrategiesMIList));

        try {
            mailService.sendMail(dailySupervisoryMI, dailyStrategiesMIMap);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
}
