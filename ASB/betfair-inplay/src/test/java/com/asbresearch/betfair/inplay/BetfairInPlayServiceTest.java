package com.asbresearch.betfair.inplay;

import com.asbresearch.betfair.inplay.model.InPlayRequest;
import com.asbresearch.betfair.inplay.model.MatchScore;
import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.config.AppConfig;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import com.asbresearch.common.CredentialsUtility;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.common.config.BigQueryProperties;
import com.asbresearch.common.config.EmailProperties;
import com.asbresearch.common.notification.EmailNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQueryOptions;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class BetfairInPlayServiceTest {

    @Ignore
    @Test
    public void testLiveService() throws LoginException, InterruptedException {
        AppConfig appConfig = new AppConfig("ZBsLSgTAiftsAy2R", "fdr@asbresearch.com", "asbcheqai87");
        BetfairReferenceClient client = new BetfairReferenceClient(Exchange.UK, appConfig.getAppKey());
        client.login(appConfig.getUserName(), appConfig.getPassword());
        BetfairLiveEventService betfairLiveEventService = new BetfairLiveEventService(client, null);
        int counter = 0;
        while (true) {
            log.info("Betfair Live Event Ids={}", betfairLiveEventService.getLiveEventIds());
            TimeUnit.SECONDS.sleep(10);
            counter++;
            if ( counter > 2) {
                break;
            }
        }
        betfairLiveEventService.stop();
    }

    @Ignore
    @Test
    public void testInPlayWithSavingToBQ() throws Exception {
        BigQueryService bgs = createBigQueryService();
        BetfairInPlayService inPlayService = new BetfairInPlayService(60000, bgs, true, true, Logger.Level.FULL);

        List<Integer> events = Arrays.asList(30652509, 30650660, 30648995);

        Set<InPlayRequest> inPlayRequests = events.stream().map(InPlayRequest::of).collect(Collectors.toSet());
        inPlayService.requestPolling(inPlayRequests);
        while (true) {
            inPlayRequests.forEach(inPlayRequest -> {
                MatchScore matchScore = inPlayService.score(inPlayRequest.getEventId()).orElse(new MatchScore.Builder().build());
                log.info("request={}, inPlay={} score={}",
                        inPlayRequest.getEventId(),
                        inPlayService.isInPlay(inPlayRequest.getEventId()), matchScore.currentScore());
            });
            TimeUnit.SECONDS.sleep(60);
        }
    }

    @Ignore
    @Test
    public void testInPlayScore() throws Exception {
        BetfairInPlayService inplayService = new BetfairInPlayService();
        Set<InPlayRequest> requests = getRequests(29764910, 29764909);
        inplayService.requestPolling(requests);
        TimeUnit.SECONDS.sleep(5);

        while (true) {
            for (InPlayRequest request : requests) {
                Integer eventId = request.getEventId();
                if (inplayService.isInPlay(eventId)) {
                    List<String> scoreHistory = inplayService.score(eventId).get().scores();
                    Optional<Instant> kickOffTime = inplayService.kickOffTime(eventId);
                    Optional<Instant> secondHalfKickOffTime = inplayService.secondHalfKickOffTime(eventId);
                    log.info("{} eventId={}, scoreHistory={} kickOff={} halfTimeKickOff={}",
                            Instant.now(),
                            eventId,
                            scoreHistory,
                            kickOffTime.isPresent() ? kickOffTime.get() : "",
                            secondHalfKickOffTime.isPresent() ? secondHalfKickOffTime.get() : "");
                } else {
                    log.info("{} not in play", eventId);
                }
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }

    private Set<InPlayRequest> getRequests(Integer... eventIds) {
        return Arrays.stream(eventIds).map(InPlayRequest::of).collect(toSet());
    }

    private BigQueryService createBigQueryService() throws IOException {
        BigQueryProperties bigQueryProperties = new BigQueryProperties();
        String location = "D:/ASBResearch/asb-pulse/data/credentials/asbresearch-dev-4dd04fe6ed02.json";
        bigQueryProperties.getCredentials().setLocation(location);
        bigQueryProperties.getSecondaryCredentials().setLocation(location);
        String projectId = "asbresearch-dev";
        bigQueryProperties.setProjectId(projectId);
        bigQueryProperties.setSecondaryProjectId(projectId);
        bigQueryProperties.setBackupPath("D:/ASBResearch/asb-pulse/data/bq");

        EmailProperties emailProperties = new EmailProperties();
        EmailNotifier emailNotifier = new EmailNotifier(new JavaMailSenderImpl(), new ObjectMapper(), emailProperties);
        BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(CredentialsUtility.googleCredentials(bigQueryProperties.getCredentials().getLocation()))
                .build();
        return new BigQueryService(bigQueryOptions.getService(), bigQueryProperties, emailProperties, emailNotifier);
    }
}