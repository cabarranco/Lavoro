package com.asbresearch.pulse.config;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider;
import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider.SSO_HOST_COM;

@EnableConfigurationProperties({AccountProperties.class, EsaProperties.class})
@Configuration
@Slf4j
public class BetfairClientsConfig {
    private final AccountProperties accountProperties;
    private final EsaProperties esaProperties;

    @Autowired
    public BetfairClientsConfig(AccountProperties accountProperties, EsaProperties esaProperties) {
        this.accountProperties = accountProperties;
        this.esaProperties = esaProperties;
    }

    public Client createEsaClient() {
        AppKeyAndSessionProvider sessionProvider = new AppKeyAndSessionProvider(SSO_HOST_COM,
                accountProperties.getAppKey(),
                accountProperties.getUser(),
                accountProperties.getPassword());
        Client client = new Client(esaProperties.getHost(), esaProperties.getPort(), sessionProvider);
        client.setTraceChangeTruncation(500);
        return client;
    }

    @Bean
    public BetfairInPlayService BetfairInPlayService() {
        return new BetfairInPlayService();
    }

    @Bean
    public BetfairReferenceClient betfairClient() throws LoginException {
        BetfairReferenceClient client = new BetfairReferenceClient(Exchange.UK, accountProperties.getAppKey());
        log.info("BetfairReferenceClient trying to login using username={}", accountProperties.getUser());
        if (!client.login(accountProperties.getUser(), accountProperties.getPassword())) {
            throw new RuntimeException("Login failure");
        }
        log.info("BetfairReferenceClient logged in successfully");
        return client;
    }
}
