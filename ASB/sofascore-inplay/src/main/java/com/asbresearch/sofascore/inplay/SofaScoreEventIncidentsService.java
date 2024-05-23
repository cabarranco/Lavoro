package com.asbresearch.sofascore.inplay;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.task.SofaScoreEventIncidentsTask;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.Timer;

import static com.asbresearch.common.Constants.DATE_FORMATTER;
import static com.asbresearch.sofascore.inplay.util.SofaScoreConstant.BASE_URL;
import static feign.Logger.Level.BASIC;

@Slf4j
public class SofaScoreEventIncidentsService {
    private static final long DEFAULT_POLLING_IN_MS = 60000;
    private final Timer eventIncidentsWorker;

    public SofaScoreEventIncidentsService(BigQueryService bigQueryService) {
        this(bigQueryService, DEFAULT_POLLING_IN_MS, LocalDate.now().minusDays(1).format(DATE_FORMATTER));
    }

    public SofaScoreEventIncidentsService(BigQueryService bigQueryService, long pollingFrequencyInMillis, String startDate) {
        this(bigQueryService, pollingFrequencyInMillis, BASIC, startDate);
    }

    public SofaScoreEventIncidentsService(BigQueryService bigQueryService,
                                          long pollingFrequencyInMillis,
                                          Logger.Level loggerLevel, String startDate) {
        SofaScoreIncidentClient sofaScoreIncidentClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(SofaScoreIncidentClient.class))
                .logLevel(loggerLevel)
                .target(SofaScoreIncidentClient.class, BASE_URL);
        log.info("Created with pollingFrequencyInMillis={}", pollingFrequencyInMillis);
        eventIncidentsWorker = new Timer("SofaScore-Event-Incidents");
        eventIncidentsWorker.scheduleAtFixedRate(new SofaScoreEventIncidentsTask(bigQueryService, sofaScoreIncidentClient, startDate), 0, pollingFrequencyInMillis);
    }

    @PreDestroy
    public void stop() {
        eventIncidentsWorker.cancel();
    }
}