package com.asbresearch.pulse.service.account;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
@ConditionalOnProperty(prefix = "account", name = "simulated", havingValue = "on")
public class SimulatedAccountService extends AbstractAccountBalanceService implements AccountBalanceService {
    private final BigQueryService bigQueryService;
    private final EmailNotifier emailNotifier;
    private final EmailProperties emailProperties;
    private final AccountProperties accountProperties;
    private final AppProperties appProperties;

    @Autowired
    public SimulatedAccountService(ObjectMapper objectMapper,
                                   AppProperties appProperties,
                                   BigQueryService bigQueryService,
                                   AccountProperties accountProperties,
                                   EmailNotifier emailNotifier,
                                   EmailProperties emailProperties) {
        super(objectMapper, appProperties, bigQueryService, accountProperties);
        this.bigQueryService = bigQueryService;
        this.emailNotifier = emailNotifier;
        this.emailProperties = emailProperties;
        this.accountProperties = accountProperties;
        this.appProperties = appProperties;
    }

    @Override
    public double getAvailableToBetBalance() {
        int emailCounter = 0;
        while (true) {
            String sql = String.format("select tradingDayAvailableBalance from `research.sim_account_balance` where node='%s' and date(datetime) <= current_date() order by datetime desc limit 1", appProperties.getNode());
            log.info("Running sql={}", sql);
            try {
                List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
                if (!isEmpty(resultSet)) {
                    Optional<Object> tradingDayAvailableBalance = resultSet.iterator().next().get("tradingDayAvailableBalance");
                    if (tradingDayAvailableBalance.isPresent()) {
                        return Double.parseDouble((String) tradingDayAvailableBalance.get());
                    }
                }
            } catch (InterruptedException e) {
                log.error("Interrupted with executing bigQuery sql={}", sql, e);
            }
            String message = String.format("Error getting accountFunds from BigQuery for user=%s node=%s", accountProperties.getUser(), appProperties.getNode());
            if (emailCounter == 0) {
                emailNotifier.sendMessageAsync(message, "Pulse: Error getting account fund details from BigQuery", emailProperties.getTo());
                emailCounter++;
            }
            RetryUtil.retryWait(message);
        }
    }
}
