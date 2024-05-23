package com.asbresearch.pulse.service.account;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.AccountFundsResponse;
import com.asbresearch.betfair.ref.enums.Wallet;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.util.RetryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
@ConditionalOnProperty(prefix = "account", name = "betfair", havingValue = "on")
public class BetfairWalletAccountService extends AbstractAccountBalanceService implements AccountBalanceService {
    private final BetfairReferenceClient betfairReferenceClient;
    private final EmailNotifier emailNotifier;
    private final EmailProperties emailProperties;
    private final AccountProperties accountProperties;

    @Autowired
    public BetfairWalletAccountService(ObjectMapper objectMapper,
                                       AppProperties appProperties,
                                       BetfairReferenceClient betfairReferenceClient,
                                       EmailNotifier emailNotifier,
                                       EmailProperties emailProperties,
                                       AccountProperties accountProperties,
                                       BigQueryService bigQueryService) {

        super(objectMapper, appProperties, bigQueryService, accountProperties);

        this.betfairReferenceClient = betfairReferenceClient;
        this.emailNotifier = emailNotifier;
        this.emailProperties = emailProperties;
        this.accountProperties = accountProperties;
    }

    @Override
    public double getAvailableToBetBalance() {
        int emailCounter = 0;
        while (true) {
            BetfairServerResponse<AccountFundsResponse> response = betfairReferenceClient.getAccountFunds(Wallet.UK);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().getAvailableToBetBalance();
            }
            String message = String.format("Error getting accountFunds from Betfair for user=%s", accountProperties.getUser());
            if( emailCounter == 0) {
                emailNotifier.sendMessageAsync(message, "Pulse: Error getting account fund details from Betfair", emailProperties.getTo());
                emailCounter++;
            }
            RetryUtil.retryWait(message);
        }
    }
}
