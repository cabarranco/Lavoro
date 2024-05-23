package com.asbresearch.pulse.service.plm;

import com.asbresearch.betfair.ref.entities.PlaceInstructionReport;
import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.service.OpportunityQueue;
import com.asbresearch.pulse.service.oms.OrderManager;
import com.asbresearch.pulse.service.strategy.StrategyCache;
import com.asbresearch.pulse.util.ThreadUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.asbresearch.betfair.ref.enums.InstructionReportStatus.SUCCESS;
import static com.asbresearch.betfair.ref.enums.Side.BACK;
import static com.asbresearch.betfair.ref.enums.Side.LAY;
import static com.asbresearch.pulse.util.Constants.MCD_EVENT_ID;
import static com.asbresearch.pulse.util.Constants.MCD_STRATEGY_ID;
import static com.asbresearch.pulse.util.Constants.OPPORTUNITY_ID;
import static com.asbresearch.pulse.util.Constants.PULSE_REPORTING;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.ZoneOffset.UTC;

@EnableConfigurationProperties({AccountProperties.class})
@Component("plmEngine")
@Slf4j
public class PlmEngine {
    private final OpportunityQueue opportunityQueue;
    private final ExecutorService worker;
    private final AccountAllocations accountAllocations;
    private final OrderManager orderManager;
    private final ConcentrationTables concentrationTables;
    private final StrategyCache strategyCache;
    private List<OpportunityBet> cycleFailures;
    private final EmailNotifier emailNotifier;
    private final EmailProperties emailProperties;
    private final BigQueryService bigQueryService;
    private final AccountProperties accountProperties;
    private final AtomicBoolean emailSent = new AtomicBoolean(false);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    public PlmEngine(StrategyCache strategyCache,
                     OrderManager orderManager,
                     AccountAllocations accountAllocations,
                     OpportunityQueue opportunityQueue,
                     ConcentrationTables concentrationTables,
                     EmailNotifier emailNotifier,
                     EmailProperties emailProperties,
                     BigQueryService bigQueryService,
                     BigQueryProperties bigQueryProperties,
                     AccountProperties accountProperties) {

        checkNotNull(accountProperties, "accountProperties must be provided");
        checkNotNull(bigQueryProperties, "bigQueryProperties must be provided");
        checkNotNull(bigQueryService, "bigQueryService must be provided");
        checkNotNull(emailProperties, "emailProperties must be provided");
        checkNotNull(emailNotifier, "emailNotifier must be provided");
        checkNotNull(strategyCache, "strategyCache must be provided");
        checkNotNull(orderManager, "orderManager must be provided");
        checkNotNull(accountAllocations, "accountAllocations must be provided");
        checkNotNull(opportunityQueue, "opportunityQueue must be provided");
        checkNotNull(concentrationTables, "concentrationTables must be provided");

        this.bigQueryService = bigQueryService;
        this.emailProperties = emailProperties;
        this.emailNotifier = emailNotifier;
        this.orderManager = orderManager;
        this.accountAllocations = accountAllocations;
        this.opportunityQueue = opportunityQueue;
        this.concentrationTables = concentrationTables;
        this.strategyCache = strategyCache;
        this.accountProperties = accountProperties;
        worker = Executors.newSingleThreadScheduledExecutor(ThreadUtils.threadFactoryBuilder("plm").build());
    }

    @PreDestroy
    public void shutDown() {
        log.info("Stopping PlmEngine");
        stop.set(true);
        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for PlmEngine to shutdown");
            Thread.currentThread().interrupt();
        }
        if (worker != null) {
            worker.shutdown();
        }
        alertShutDown();
    }

    private void alertShutDown() {
        Instant now = Instant.now();
        String subject = String.format("%s Pulse killed as of %s", activeProfile.toUpperCase(), now);
        emailNotifier.sendMessage(subject, subject, emailProperties.getTo());
    }

    @PostConstruct
    public void start() {
        concentrationTables.init(strategyCache.getStrategies());
        log.info("ConcentrationTables Summary events={} strategies={} strategyEvents={}",
                concentrationTables.getEvents().getRecords().size(),
                concentrationTables.getStrategies().getRecords().size(),
                concentrationTables.getStrategyEvents().getRecords().size());
        refreshConcentrationTablesFromPreviousBets(strategyCache.getStrategyIds());
        worker.submit(() -> drainAndProcessOpportunitiesFromQueue());
    }

    private void refreshConcentrationTablesFromPreviousBets(Set<String> strategyIds) {
        ZonedDateTime startTime = LocalDate.now().atStartOfDay(UTC).plusHours(4);
        String query = String.format("select strategyId, eventId, orderSide, betAmount, betPrice from `%s.orders_bets` where UNIX_MILLIS(orderTimestamp) BETWEEN %s and %s  AND orderStatus = 'SUCCESS'",
                PULSE_REPORTING, startTime.toInstant().toEpochMilli(), startTime.plusHours(24).toInstant().toEpochMilli());
        if (!CollectionUtils.isEmpty(strategyIds)) {
            query = String.format("%s AND strategyId in (%s)", query, strategyIds.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(",")));
        }
        try {
            List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(query);
            rows.forEach(row -> {
                Optional<Object> optSide = row.get("orderSide");
                if (optSide.isPresent()) {
                    double usedBalance = 0.0;
                    Optional<Object> betAmount = row.get("betAmount");
                    Optional<Object> betPrice = row.get("betPrice");
                    if (BACK == Side.valueOf(optSide.get().toString()) && betAmount.isPresent()) {
                        usedBalance = Double.valueOf(betAmount.get().toString());
                    }
                    if (LAY == Side.valueOf(optSide.get().toString()) && betAmount.isPresent() && betPrice.isPresent()) {
                        usedBalance = Precision.round(Double.valueOf(betAmount.get().toString()) * Double.valueOf(betPrice.get().toString()), 2);
                    }
                    Optional<Object> eventId = row.get("eventId");
                    if (eventId.isPresent()) {
                        concentrationTables.updateEventConcentration(eventId.get().toString(), usedBalance);
                    }
                    Optional<Object> strategyId = row.get("strategyId");
                    if (strategyId.isPresent()) {
                        concentrationTables.updateStrategyConcentration(strategyId.get().toString(), usedBalance);
                    }
                    if (eventId.isPresent() && strategyId.isPresent()) {
                        concentrationTables.updateStrategyEventConcentration(strategyId.get().toString(), eventId.get().toString(), usedBalance);
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    private void drainAndProcessOpportunitiesFromQueue() {
        cycleFailures = new ArrayList<>();
        OpportunityBet nextOpportunity = null;
        while (!stop.get()) {
            try {
                List<OpportunityBet> opportunityBets = opportunityQueue.availableOpportunities(cycleFailures);
                cycleFailures.clear();
                if (nextOpportunity != null) {
                    opportunityBets.add(nextOpportunity);
                }
                if (!opportunityBets.isEmpty()) {
                    Optional<OpportunityBet> opportunityBet = processCurrentBetOpportunities(opportunityBets);
                    if (opportunityBet.isPresent()) {
                        try {
                            String eventId = opportunityBet.get().getEvent().getId();
                            String strategyId = opportunityBet.get().getStrategyId();
                            String opportunityId = opportunityBet.get().getOpportunityId();
                            setUpMdcContext(eventId, strategyId, opportunityId);
                            List<PlaceInstructionReport> placeInstructionReports = orderManager.placeOrders(opportunityBet.get());
                            if (!placeInstructionReports.isEmpty()) {
                                accountAllocations.updateAvailableBalanceToBet();
                                double usedBalance = calcUsedBalance(placeInstructionReports);
                                concentrationTables.updateEventConcentration(eventId, usedBalance);
                                concentrationTables.updateStrategyConcentration(strategyId, usedBalance);
                                concentrationTables.updateStrategyEventConcentration(strategyId, eventId, usedBalance);
                                log.debug("concentrationTables={}", concentrationTables);
                            }
                        } finally {
                            MDC.clear();
                        }
                    }
                }
                nextOpportunity = opportunityQueue.nextAvailableOpportunity();
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for bet opportunities", e);
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException re) {
                log.error("RuntimeException while processing bet opportunities", re);
            }
        }
        stopLatch.countDown();
    }

    public static void setUpMdcContext(String eventId, String strategyId, String opportunityId) {
        MDC.clear();
        MDC.put(OPPORTUNITY_ID, opportunityId);
        MDC.put(MCD_STRATEGY_ID, strategyId);
        MDC.put(MCD_EVENT_ID, eventId);
    }

    private double calcUsedBalance(List<PlaceInstructionReport> placeInstructionReports) {
        return placeInstructionReports.stream()
                .map(placeInstructionReport -> calcUsedBalance(placeInstructionReport))
                .mapToDouble(value -> value.doubleValue())
                .sum();
    }

    private double calcUsedBalance(PlaceInstructionReport placeInstructionReport) {
        if (SUCCESS == placeInstructionReport.getStatus()) {
            if (BACK == placeInstructionReport.getInstruction().getSide()) {
                return placeInstructionReport.getSizeMatched();
            } else {
                return placeInstructionReport.getSizeMatched() * placeInstructionReport.getAveragePriceMatched();
            }
        }
        return 0.0;
    }

    private Optional<OpportunityBet> processCurrentBetOpportunities(List<OpportunityBet> opportunityBets) {
        log.info("availableToBetBalance={} opportunityMaxAllocationSum={}",
                accountAllocations.getAvailableToBetBalance(), accountAllocations.getOpportunityMaxAllocationSum());
        if (accountAllocations.getAvailableToBetBalance() < accountAllocations.getOpportunityMaxAllocationSum()) {
            sendAccountDepleteNotification();
            accountAllocations.updateAvailableBalanceToBet();
            return Optional.empty();
        } else {
            resetSentMail();
        }
        List<OpportunityBet> filteredBets = opportunityBets.stream().filter(this::isMaxAllocationSumGreaterThanConcentrationLimits).collect(Collectors.toList());
        if (filteredBets.isEmpty()) {
            return Optional.empty();
        }
        if (filteredBets.size() == 1) {
            return Optional.of(filteredBets.get(0));
        }
        List<OpportunityBet> rankedBets = new ImpliedProbMinutesToEndRanking(Instant.now(), filteredBets).rank();
        opportunityQueue.add(rankedBets.stream().skip(1).collect(Collectors.toList()));
        return Optional.of(rankedBets.get(0));
    }

    private void resetSentMail() {
        if (emailSent.get() == true) {
            emailSent.getAndSet(false);
        }
    }

    private void sendAccountDepleteNotification() {
        if (emailSent.get() == false) {
            log.warn("availableToBetBalance={}  is lessThan opportunityMaxAllocationSum={} ...Sending alert email", accountAllocations.getAvailableToBetBalance(), accountAllocations.getOpportunityMaxAllocationSum());
            Instant now = Instant.now();
            String subject = String.format("%s Pulse As of %s; No Available Balance", activeProfile.toUpperCase(), now);
            List<String> lines = new ArrayList<>();
            lines.add(subject);
            lines.add("Betfair");
            lines.add(String.format("user = %s", accountProperties.getUser()));
            lines.add(String.format("availableToBetBalance = %s", accountAllocations.getAvailableToBetBalance()));
            lines.add(String.format("balanceSaving = %s", accountAllocations.getBalanceSaving()));
            lines.add(String.format("opportunityMaxAllocationSum = %s", accountAllocations.getOpportunityMaxAllocationSum()));
            Precision.round(accountAllocations.getBalanceSaving() / accountProperties.getPercentageBalanceToSave(), 2);
            lines.add(String.format("Initial tradingDayAvailableBalance = %s", Precision.round(accountAllocations.getBalanceSaving() / accountProperties.getPercentageBalanceToSave(), 2)));
            emailNotifier.sendMessageAsync(lines.stream().collect(Collectors.joining("\n\n")), subject, emailProperties.getTo());
            emailSent.set(true);
        }
    }

    private boolean isMaxAllocationSumGreaterThanConcentrationLimits(OpportunityBet opportunityBet) {
        ConcentrationRecord concentrationRecord = concentrationTables.getEvents().get(opportunityBet.getEvent().getId());
        double opportunityMaxAllocationSum = accountAllocations.getOpportunityMaxAllocationSum();
        if (opportunityMaxAllocationSum > (concentrationRecord.getMaxTradingDayAvailableBalance() - concentrationRecord.getUsedBalance())) {
            log.warn("Event opportunityMaxAllocationSum={}  is greater than (concentrationRecord.getMaxTradingDayAvailableBalance()={} minus concentrationRecord.getUsedBalance()={}) for opportunityBet={}",
                    opportunityMaxAllocationSum, concentrationRecord.getMaxTradingDayAvailableBalance(), concentrationRecord.getUsedBalance(), opportunityBet);
            return false;
        }
        concentrationRecord = concentrationTables.getStrategies().get(opportunityBet.getStrategyId());
        if (opportunityMaxAllocationSum > (concentrationRecord.getMaxTradingDayAvailableBalance() - concentrationRecord.getUsedBalance())) {
            log.warn("Strategy opportunityMaxAllocationSum={}  is greater than (concentrationRecord.getMaxTradingDayAvailableBalance()={} minus concentrationRecord.getUsedBalance()={} for opportunityBet={})",
                    opportunityMaxAllocationSum, concentrationRecord.getMaxTradingDayAvailableBalance(), concentrationRecord.getUsedBalance(), opportunityBet);
            return false;
        }
        concentrationRecord = concentrationTables.getStrategyEvents().get(String.format("%s-%s", opportunityBet.getStrategyId(), opportunityBet.getEvent().getId()));
        if (opportunityMaxAllocationSum > (concentrationRecord.getMaxTradingDayAvailableBalance() - concentrationRecord.getUsedBalance())) {
            log.warn("StrategyEvent opportunityMaxAllocationSum={}  is greater than (concentrationRecord.getMaxTradingDayAvailableBalance()={} minus concentrationRecord.getUsedBalance()={} for opportunityBet={})",
                    opportunityMaxAllocationSum, concentrationRecord.getMaxTradingDayAvailableBalance(), concentrationRecord.getUsedBalance(), opportunityBet);
            return false;
        }
        return true;
    }
}
