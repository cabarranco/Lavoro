package com.asbresearch.pulse.service.plm;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.AccountDetailsResponse;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.entity.AccountBalance;
import com.asbresearch.pulse.service.account.AccountBalanceService;
import com.asbresearch.pulse.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
@EnableConfigurationProperties({AccountProperties.class, StrategyProperties.class, EmailProperties.class, AppProperties.class})
@Slf4j
public class AccountAllocations {
    private final EmailProperties emailProperties;
    private final double balanceSaving;
    private final double opportunityMaxAllocationSum;
    private final AccountBalanceService accountBalanceService;
    private final BetfairReferenceClient betfairReferenceClient;
    private final AccountProperties accountProperties;
    private final EmailNotifier emailNotifier;
    private volatile double availableToBetBalance;

    @Autowired
    public AccountAllocations(AccountBalanceService accountBalanceService,
                              BetfairReferenceClient betfairReferenceClient,
                              AccountProperties accountProperties,
                              EmailProperties emailProperties,
                              EmailNotifier emailNotifier) {
        checkNotNull(emailProperties, "emailProperties must be provided");
        checkNotNull(accountProperties, "accountProperties must be provided");
        checkNotNull(betfairReferenceClient, "betfairReferenceClient must be provided");
        checkNotNull(emailNotifier, "emailNotifier must be provided");

        this.emailProperties = emailProperties;
        this.accountBalanceService = accountBalanceService;
        this.betfairReferenceClient = betfairReferenceClient;
        this.accountProperties = accountProperties;
        this.emailNotifier = emailNotifier;

        balanceSaving = calcBalanceSaving();
        availableToBetBalance = Precision.round(accountBalanceService.getAvailableToBetBalance() - balanceSaving, 2);
        opportunityMaxAllocationSum = calcOpportunityMaxAllocationSum(availableToBetBalance, accountProperties);
        if (opportunityMaxAllocationSum * accountProperties.getMinDayBets() >= availableToBetBalance) {
            String message = String.format("minAllocationSum=%s multiply by minDayBets=%s is greater than/equal availableToBetBalance=%s",
                    availableToBetBalance,
                    opportunityMaxAllocationSum,
                    accountProperties.getMinDayBets());
            emailNotifier.sendMessageAsync(message, "Pulse: Account Balance Notification", emailProperties.getTo());
        }
        if (accountProperties.getMaxEventConcentration() == null) {
            accountProperties.setMaxEventConcentration((opportunityMaxAllocationSum + 1) / availableToBetBalance);
        }
        if (accountProperties.getMaxStrategyEventConcentration() == null) {
            accountProperties.setMaxStrategyEventConcentration((opportunityMaxAllocationSum + 1) / availableToBetBalance);
        }
        log.info("balanceSaving={} availableToBetBalance={} opportunityMaxAllocationSum={} eventConcentration={} strategyEventConcentration={}",
                balanceSaving,
                availableToBetBalance,
                opportunityMaxAllocationSum,
                accountProperties.getMaxEventConcentration(),
                accountProperties.getMaxStrategyEventConcentration());
    }

    private double calcBalanceSaving() {
        int emailCounter = 0;
        while (true) {
            try {
                Instant now = Instant.now();
                double availableToBet;
                Optional<AccountBalance> accountBalanceOpt = accountBalanceService.find(accountProperties.getUser(), now);
                if (accountBalanceOpt.isPresent()) {
                    availableToBet = accountBalanceOpt.get().getAvailableToBet();
                } else {
                    availableToBet = accountBalanceService.getAvailableToBetBalance();
                    AccountDetailsResponse accountDetails = getAccountDetails();
                    AccountBalance accountBalance = new AccountBalance(accountProperties.getUser(), now, availableToBet, accountDetails.getCurrencyCode());
                    accountBalanceService.create(accountBalance);
                    accountBalanceService.updateBigQueryAccountBalance(accountBalance);
                }
                return Precision.round(availableToBet * accountProperties.getPercentageBalanceToSave(), 2);
            } catch (Exception ex) {
                String message = String.format("Error trying to get start trading day balance for user=%s", accountProperties.getUser());
                if( emailCounter == 0) {
                    emailNotifier.sendMessageAsync(String.format("%s cause=%s", message, ex), "Pulse: Account Balance Notification", emailProperties.getTo());
                    emailCounter++;
                }
                RetryUtil.retryWait(message, ex);
            }
        }
    }

    private AccountDetailsResponse getAccountDetails() {
        int emailCounter = 0;
        while (true) {
            BetfairServerResponse<AccountDetailsResponse> response = betfairReferenceClient.getAccountDetails();
            if (response != null && response.getResponse() != null) {
                return response.getResponse();
            }
            String message = String.format("Error getting accountDetails from Betfair for user=%s", accountProperties.getUser());
            if (emailCounter == 0) {
                emailNotifier.sendMessageAsync(message, "Pulse: Error getting accountDetails from Betfair", emailProperties.getTo());
                emailCounter++;
            }
            RetryUtil.retryWait(message);
        }
    }

    protected double calcOpportunityMaxAllocationSum(double startTradingDayAvailableBalance, AccountProperties accountProperties) {
        double result = Precision.round(startTradingDayAvailableBalance / accountProperties.getMaxAllocationSplitter(), 2);
        return Math.max(accountProperties.getOpportunityMinAllocationSum(), result);
    }

    public double getAvailableToBetBalance() {
        return availableToBetBalance;
    }

    public double getBalanceSaving() {
        return balanceSaving;
    }

    public double getOpportunityMaxAllocationSum() {
        return opportunityMaxAllocationSum;
    }

    public void updateAvailableBalanceToBet() {
        availableToBetBalance = accountBalanceService.getAvailableToBetBalance() - balanceSaving;
        log.info("Updating availableToBetBalance={}", availableToBetBalance);
    }
}
