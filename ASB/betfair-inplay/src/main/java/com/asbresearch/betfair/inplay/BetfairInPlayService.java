package com.asbresearch.betfair.inplay;

import com.asbresearch.betfair.inplay.model.*;
import com.asbresearch.betfair.inplay.task.InPlayRequestTask;
import com.asbresearch.betfair.inplay.util.InPlayUtil;
import com.asbresearch.common.ThreadUtils;
import com.asbresearch.common.bigquery.BigQueryService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.asbresearch.betfair.inplay.util.InPlayUtil.KICK_OFF;
import static com.asbresearch.betfair.inplay.util.InPlayUtil.SECOND_HALF_KICK_OFF;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static feign.Logger.Level.BASIC;

@Slf4j
public class BetfairInPlayService {
    public static final int DEFAULT_POLLING_IN_MS = 1000;
    private static final String BASE_URL = "https://ips.betfair.com";

    private final Map<Integer, InPlayResponse> responses = new ConcurrentHashMap<>();
    private final Map<Integer, InPlayRequest> requests = new ConcurrentHashMap<>();
    private final Map<Integer, KickOffTimes> snapKickOffTimes = new ConcurrentHashMap<>();
    private final Timer inPlayRequestWorker;
    private final ExecutorService inPlayDataSaver;
    private final boolean useSnapshotTime;

    public BetfairInPlayService() {
        this(DEFAULT_POLLING_IN_MS, null, false, true, BASIC);
    }

    public BetfairInPlayService(int pollingFrequencyInMillis,
                                BigQueryService bigQueryService,
                                boolean saveToBigQuery,
                                boolean useSnapshotTime,
                                Logger.Level loggerLevel) {
        if (saveToBigQuery && bigQueryService == null) {
            throw new IllegalArgumentException("BigQueryService must be set, if saveToBigQuery is true");
        }
        BetfairInPlayClient betfairInplayClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(BetfairInPlayClient.class))
                .logLevel(loggerLevel)
                .target(BetfairInPlayClient.class, BASE_URL);


        inPlayRequestWorker = new Timer("InPlayWorker");
        inPlayDataSaver = Executors.newSingleThreadExecutor(ThreadUtils.threadFactoryBuilder("inPlayDataSaver").build());
        inPlayRequestWorker.scheduleAtFixedRate(new InPlayRequestTask(betfairInplayClient, requests, responses, snapKickOffTimes, bigQueryService, saveToBigQuery, useSnapshotTime, inPlayDataSaver), 0, pollingFrequencyInMillis);
        this.useSnapshotTime = useSnapshotTime;

        log.info("Created with pollingFrequencyInMillis={} saveToBigQuery={} useSnapshotTime={}", pollingFrequencyInMillis, saveToBigQuery, useSnapshotTime);
    }

    public void requestPolling(Set<InPlayRequest> inPlayRequests) {
        checkNotNull(inPlayRequests, "InPlayRequests must be provided");
        if (!inPlayRequests.isEmpty()) {
            Set<InPlayRequest> difference = Sets.difference(inPlayRequests, Sets.newHashSet(requests.values()));
            difference.forEach(request -> this.requests.putIfAbsent(request.getEventId(), request));
            if (!difference.isEmpty()) {
                log.info("Pending requestPolling for {} events", difference.size());
                log.debug("InPlay request size={} request={}", difference.size(), difference.stream().sorted(Comparator.comparing(InPlayRequest::getStartTime)).collect(Collectors.toList()));
            }
        }
    }

    @PreDestroy
    public void stop() {
        inPlayRequestWorker.cancel();
        if (inPlayDataSaver != null) {
            inPlayDataSaver.shutdownNow();
            try {
                inPlayDataSaver.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("SecondHalfEndLoader interrupted while waiting to shutdown");
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isInPlay(Integer eventId) {
        InPlayResponse inPlayResponse = responses.get(eventId);
        if (inPlayResponse == null) {
            return false;
        }
        return InPlayUtil.isInPlay(inPlayResponse);
    }

    public Optional<MatchScore> score(Integer eventId) {
        log.info("inPlay-score begin for event={}", eventId);
        InPlayResponse inplayResponse = responses.get(eventId);
        if (inplayResponse == null) {
            log.info("inPlay-score end 0-0");
            return Optional.empty();
        }
        MatchScore.Builder builder = new MatchScore.Builder();
        List<UpdateDetail> updateDetails = firstNonNull(inplayResponse.getUpdateDetails(), ImmutableList.of());
        updateDetails = updateDetails.stream()
                .filter(updateDetail -> "Goal".equals(updateDetail.getUpdateType()))
                .collect(Collectors.toList());
        updateDetails.forEach(updateDetail -> {
            if ("home".equals(updateDetail.getTeam())) {
                builder.homeGoal();
            } else {
                builder.awayGoal();
            }
        });
        Optional<MatchScore> score = Optional.of(builder.build());
        log.info("inPlay-score end {}", score.get().currentScore());
        return score;
    }

    public Optional<Instant> kickOffTime(Integer eventId) {
        if (useSnapshotTime) {
            KickOffTimes kickOffTimes = snapKickOffTimes.get(eventId);
            if (kickOffTimes != null) {
                return Optional.of(kickOffTimes.getKickOff());
            }
        }
        if (responses.containsKey(eventId)) {
            InPlayResponse inplayResponse = responses.get(eventId);
            Optional<UpdateDetail> kickOffUpdate = inplayResponse.getUpdateDetails()
                    .stream()
                    .filter(updateDetail -> KICK_OFF.equals(updateDetail.getUpdateType()))
                    .findAny();
            if (kickOffUpdate.isPresent()) {
                UpdateDetail updateDetail = kickOffUpdate.get();
                if (updateDetail.getUpdateTime() != null) {
                    return Optional.of(updateDetail.getUpdateTime());
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Instant> secondHalfKickOffTime(Integer eventId) {
        if (useSnapshotTime) {
            KickOffTimes kickOffTimes = snapKickOffTimes.get(eventId);
            if (kickOffTimes != null) {
                return Optional.of(kickOffTimes.getSecondHalfKickOff());
            }
        }
        if (responses.containsKey(eventId)) {
            InPlayResponse inplayResponse = responses.get(eventId);
            Optional<UpdateDetail> secondHalfKickOff = inplayResponse.getUpdateDetails()
                    .stream()
                    .filter(updateDetail -> SECOND_HALF_KICK_OFF.equals(updateDetail.getUpdateType()))
                    .findAny();
            if (secondHalfKickOff.isPresent()) {
                UpdateDetail updateDetail = secondHalfKickOff.get();
                if (updateDetail.getUpdateTime() != null) {
                    return Optional.of(updateDetail.getUpdateTime());
                }
            }
        }
        return Optional.empty();
    }
}