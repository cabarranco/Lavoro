package com.asbresearch.collector.config;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import com.asbresearch.common.config.AccountProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.config.EsaProperties;
import com.asbresearch.common.notification.EmailNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider.SSO_HOST_COM;

@Component("BetfairReferenceClient")
@EnableConfigurationProperties({AccountProperties.class, EsaProperties.class})
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "betfairReferenceClient", havingValue = "on")
public class BetfairClientsConfig {
    private final AccountProperties accountProperties;
    private final EsaProperties esaProperties;
    private final EmailProperties emailProperties;
    private final EmailNotifier emailNotifier;
    private Instant prevConnErrorNotification;

    @Autowired
    public BetfairClientsConfig(AccountProperties accountProperties,
                                EsaProperties esaProperties,
                                EmailProperties emailProperties,
                                EmailNotifier emailNotifier) {
        this.accountProperties = accountProperties;
        this.esaProperties = esaProperties;
        this.emailProperties = emailProperties;
        this.emailNotifier = emailNotifier;
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
    public BetfairReferenceClient betfairClient() throws LoginException, InterruptedException {
        BetfairReferenceClient client = new BetfairReferenceClient(Exchange.UK, accountProperties.getAppKey());
        log.info("BetfairReferenceClient trying to login using username={}", accountProperties.getUser());
        while (true) {
            try {
                Boolean loggedIn = client.login(accountProperties.getUser(), accountProperties.getPassword());
                if (loggedIn) {
                    break;
                }
            } catch (LoginException ex) {
                log.error("Error trying to login to Betfair...trying in 1 min", ex);
                Instant now = Instant.now();
                if (prevConnErrorNotification == null || Duration.between(now, prevConnErrorNotification).toHours() >= 1) {
                    prevConnErrorNotification = now;
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    String content = "Error trying to login to Betfair\n" + sw;
                    emailNotifier.sendMessageAsync(content, "Error login to Betfair", emailProperties.getTo());
                }
                TimeUnit.MINUTES.sleep(1);
            }
        }
        log.info("BetfairReferenceClient logged in successfully");
        return client;
    }
}
