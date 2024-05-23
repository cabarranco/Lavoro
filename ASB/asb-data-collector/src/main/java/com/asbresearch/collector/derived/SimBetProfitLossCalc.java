package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.collector.model.BetAllocation;
import com.asbresearch.collector.model.SimBetProfitLoss;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.asbresearch.collector.util.Constants.endDate;
import static com.asbresearch.collector.util.Constants.startDate;

@Component
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class, BigQueryProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "simBetProfitLossCalc", havingValue = "on")
public class SimBetProfitLossCalc {
    public static final String EVENT_SQL = "SELECT distinct eventId FROM `research.sim_orders` where date(orderTimestamp) >= '%s' and date(orderTimestamp) < '%s'" +
            "  except distinct " +
            "  SELECT distinct eventId FROM `research.sim_orders_pl` where date(betTimestamp) >= '%s' and date(betTimestamp) < '%s'";
    public static final String BET_OPPORTUNITY_SQL = "SELECT unix_millis(orderTimestamp) as orderTimestamp, bookRunner, orderAllocation, orderPrice, eventId, opportunityId, strategyId, eventName FROM `research.sim_orders` where date(orderTimestamp) >= '%s' and date(orderTimestamp) < '%s' and eventId in (%s) order by eventId, orderTimestamp";
    public static final String SCORE_SQL = "SELECT eventId, score FROM `betstore.sofascore_soccer_inplay` where eventId in " +
            "( select sofascoreEventId from `betstore.betfair_sofascore_event_mapping` where betfairEventId in (%s) ) and updateType = 'SecondHalfEnd' " +
            " Union All " +
            " SELECT eventId, score FROM `betstore.betfair_soccer_inplay` where eventId in (%s) and updateType = 'SecondHalfEnd'";
    public static final String BETFAIR_SOFASCORE_MAPPING_SQL = "SELECT betfairEventId, sofascoreEventId FROM `betstore.betfair_sofascore_event_mapping` where betfairEventId  in (%s)";
    public static final String MO_H_B = "MO.H.B";
    public static final String CS_00_B = "CS.00.B";
    public static final String CS_01_B = "CS.01.B";
    private final BigQueryService bigQueryService;
    private final String startDateStr;
    private final String endDateStr;

    public SimBetProfitLossCalc(BigQueryService bigQueryService,
                                CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        this.startDateStr = startDate(collectorProperties);;
        this.endDateStr = endDate(collectorProperties);
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    public void calcPricesAnalytics() {
        int insertedRows = 0;
        log.info("Begin SimBetProfitLossCalc startDate={} endDate={}", startDateStr, endDateStr);
        try {

            Map<String, String> scores = scores();
            Map<String, String> betfairSofaScoreMapping = betfairSofaScoreMapping();
            Map<String, String> eventFirstOpportunity = new HashMap<>();
            Map<String, String> opportunityEvent = new HashMap<>();
            Map<String, Map<String, BetAllocation>> bets = new HashMap<>();
            String sql = String.format(BET_OPPORTUNITY_SQL, startDateStr, endDateStr, eventSql());
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> resultSets = bigQueryService.performQuery(sql);
            resultSets.forEach(row -> {
                String eventId = row.get("eventId").get().toString();
                String eventName = row.get("eventName").get().toString();
                String opportunityId = row.get("opportunityId").get().toString();
                String strategyId = row.get("strategyId").get().toString();
                String bookRunner = row.get("bookRunner").get().toString();
                Double orderAllocation = Double.valueOf(row.get("orderAllocation").get().toString());
                Double orderPrice = Double.valueOf(row.get("orderPrice").get().toString());
                Instant betTimestamp = Instant.ofEpochMilli(Long.valueOf(row.get("orderTimestamp").get().toString()));
                if (!eventFirstOpportunity.containsKey(eventId)) {
                    eventFirstOpportunity.put(eventId, opportunityId);
                }
                opportunityEvent.put(opportunityId, eventId);
                Map<String, BetAllocation> allocations = bets.getOrDefault(opportunityId, new HashMap<>());
                BetAllocation betAllocation = BetAllocation.builder()
                        .orderAllocation(orderAllocation)
                        .orderPrice(orderPrice)
                        .bookRunner(bookRunner)
                        .eventName(eventName)
                        .betTimestamp(betTimestamp)
                        .opportunityId(opportunityId)
                        .eventId(eventId)
                        .strategyId(strategyId)
                        .build();
                allocations.put(bookRunner, betAllocation);
                bets.put(opportunityId, allocations);
            });

            List<SimBetProfitLoss> simBetProfitLosses = new ArrayList<>();
            bets.forEach((opportunityId, allocations) -> {
                Double allocation = opportunityAllocation(allocations);
                String eventId = allocations.get(MO_H_B).getEventId();
                String eventScore = scores.get(eventId);
                if ( eventScore == null) {
                    String sofaScoreId = betfairSofaScoreMapping.get(eventId);
                    eventScore = scores.get(sofaScoreId);
                }
                String winBookRunner = null;
                List<Double> pl = new ArrayList<>();
                if (eventScore != null) {
                    if (isHomeWin(eventScore)) {
                        winBookRunner = MO_H_B;
                        BetAllocation betAllocation = allocations.get(winBookRunner);
                        pl.add(betAllocation.getOrderAllocation() * (betAllocation.getOrderPrice() - 1));
                    } else {
                        BetAllocation betAllocation = allocations.get(MO_H_B);
                        pl.add(betAllocation.getOrderAllocation() * -1);
                    }

                    if (isZeroOne(eventScore)) {
                        winBookRunner = CS_01_B;
                        BetAllocation betAllocation = allocations.get(winBookRunner);
                        pl.add(betAllocation.getOrderAllocation() * (betAllocation.getOrderPrice() - 1));
                    } else {
                        BetAllocation betAllocation = allocations.get(CS_01_B);
                        pl.add(betAllocation.getOrderAllocation() * -1);
                    }
                    if (isZeroZero(eventScore)) {
                        winBookRunner = CS_00_B;
                        BetAllocation betAllocation = allocations.get(winBookRunner);
                        pl.add(betAllocation.getOrderAllocation() * (betAllocation.getOrderPrice() - 1));
                    } else {
                        BetAllocation betAllocation = allocations.get(CS_00_B);
                        pl.add(betAllocation.getOrderAllocation() * -1);
                    }
                }
                String firstOpportunity = eventFirstOpportunity.get(eventId);
                Double totalPL = null;
                if (!pl.isEmpty()) {
                    totalPL = pl.stream().mapToDouble(value -> value).sum();
                    totalPL = Precision.round(totalPL, 2);
                }
                SimBetProfitLoss simBetProfitLoss = SimBetProfitLoss.builder()
                        .allocation(Precision.round(allocation, 2))
                        .betTimestamp(allocations.get(MO_H_B).getBetTimestamp())
                        .winBookRunner(winBookRunner)
                        .isFirstBet(opportunityId.equals(firstOpportunity))
                        .pl(totalPL)
                        .eventId(eventId)
                        .eventName(allocations.get(MO_H_B).getEventName())
                        .score(eventScore)
                        .opportunityId(opportunityId)
                        .strategyId(allocations.get(MO_H_B).getStrategyId())
                        .build();
                simBetProfitLosses.add(simBetProfitLoss);
            });
            bigQueryService.insertRows("research", "sim_orders_pl", simBetProfitLosses.stream().map(SimBetProfitLoss::toCsv).collect(Collectors.toList()));
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error occurred with processing SimBetProfitLossCalc", e);
        } finally {
            log.info("End SimBetProfitLossCalc with totalRows={} for startDate={} endDate={}", insertedRows, startDateStr, endDateStr);
        }
    }

    private Double opportunityAllocation(Map<String, BetAllocation> allocations) {
        return allocations.values()
                .stream()
                .map(BetAllocation::getOrderAllocation).reduce(0.0, Double::sum);
    }

    private boolean isZeroZero(String eventScore) {
        String[] token = eventScore.split("-");
        int homeGoals = Integer.parseInt(token[0]);
        int awayGoals = Integer.parseInt(token[1]);
        return homeGoals == 0 && awayGoals == 0;
    }

    private boolean isZeroOne(String eventScore) {
        String[] token = eventScore.split("-");
        int homeGoals = Integer.parseInt(token[0]);
        int awayGoals = Integer.parseInt(token[1]);
        return homeGoals == 0 && awayGoals == 1;
    }

    private boolean isHomeWin(String eventScore) {
        String[] token = eventScore.split("-");
        int homeGoals = Integer.parseInt(token[0]);
        int awayGoals = Integer.parseInt(token[1]);
        return homeGoals > awayGoals;
    }

    private Map<String, String> betfairSofaScoreMapping() throws InterruptedException {
        Map<String, String> result = new HashMap<>();
        String sql = String.format(BETFAIR_SOFASCORE_MAPPING_SQL, eventSql());
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
        resultSet.forEach(row -> {
            String eventId = row.get("betfairEventId").get().toString();
            String score = row.get("sofascoreEventId").get().toString();
            result.put(eventId, score);
        });
        return result;
    }

    private Map<String, List<BetAllocation>> betOpportunities() throws InterruptedException {
        Map<String, List<BetAllocation>> result = new HashMap<>();
        String sql = String.format(BET_OPPORTUNITY_SQL, startDateStr, endDateStr, eventSql());
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
        resultSet.forEach(row -> {
            String eventId = row.get("eventId").get().toString();
            boolean isFirstBet = false;
            List<BetAllocation> bets = result.getOrDefault(eventId, new ArrayList<>());
            if (bets.isEmpty()) {
                isFirstBet = true;
            }
            BetAllocation betOpportunity = BetAllocation.builder()
                    .eventId(eventId)
                    .opportunityId(row.get("opportunityId").get().toString())
                    .strategyId((row.get("strategyId").get().toString()))
                    .isFirstBet(isFirstBet)
                    .betTimestamp(Instant.ofEpochMilli(Long.valueOf(row.get("orderTimestamp").get().toString())))
                    .build();
            bets.add(betOpportunity);
            result.put(eventId, bets);
        });
        return result;
    }

    private Map<String, String> scores() throws InterruptedException {
        Map<String, String> result = new HashMap<>();
        String sql = String.format(SCORE_SQL, eventSql(), eventSql());
        log.info("sql={}", sql);
        List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
        resultSet.forEach(row -> {
            String eventId = row.get("eventId").get().toString();
            String score = row.get("score").get().toString();
            result.put(eventId, score);
        });
        return result;
    }

    private String eventSql() {
        return String.format(EVENT_SQL, startDateStr, endDateStr, startDateStr, endDateStr);
    }
}
