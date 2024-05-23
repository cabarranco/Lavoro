package com.asbresearch.betfair.inplay.task;

import com.asbresearch.betfair.inplay.BetfairInPlayClient;
import com.asbresearch.betfair.inplay.model.*;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.asbresearch.betfair.inplay.model.SoccerInplayRecord.TABLE;
import static com.asbresearch.betfair.inplay.util.InPlayUtil.*;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static com.asbresearch.common.Constants.partitionBasedOnSize;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
public class InPlayRequestTask extends TimerTask {
    private static final String EVENT_IDS = "eventIds";
    private static final Map<String, String> stdParams = Map.of("_ak", "nzIFcwyWhrlwYMrh", "alt", "json", "locale", "en");
    private static final String KICK_OFF_TIME_SQL = String.format("SELECT  '%%s' as eventId, " +
                    "  (select unix_millis(min(publishTime)) FROM `%s.betfair_historical_data` WHERE eventId = '%%s' and inplay is true and DATE(publishTime) = current_date()) inPlayStart, " +
                    "  (select unix_millis(max(publishTime)) FROM `%s.betfair_historical_data` WHERE eventId = '%%s' and inplay is false and DATE(publishTime) = current_date()) preLiveEnd",
            BETSTORE_DATASET, BETSTORE_DATASET);

    private final BetfairInPlayClient betfairInplayClient;
    private final Map<Integer, InPlayRequest> requests;
    private final Map<Integer, InPlayResponse> responses;
    private final Map<Integer, KickOffTimes> snapKickOffTimes;
    private final Map<Integer, Boolean> completedGames = new ConcurrentHashMap<>();
    private final BigQueryService bigQueryService;
    private final boolean saveToBigQuery;
    private final boolean useSnapshotTime;
    private final ExecutorService inPlayDataSaver;

    public InPlayRequestTask(BetfairInPlayClient betfairInplayClient,
                             Map<Integer, InPlayRequest> requests,
                             Map<Integer, InPlayResponse> responses,
                             Map<Integer, KickOffTimes> snapKickOffTimes,
                             BigQueryService bigQueryService,
                             boolean saveToBigQuery,
                             boolean useSnapshotTime,
                             ExecutorService inPlayDataSaver) {
        this.betfairInplayClient = betfairInplayClient;
        this.requests = requests;
        this.responses = responses;
        this.bigQueryService = bigQueryService;
        this.saveToBigQuery = saveToBigQuery;
        this.useSnapshotTime = useSnapshotTime;
        this.inPlayDataSaver = inPlayDataSaver;
        this.snapKickOffTimes = snapKickOffTimes;
    }

    @Override
    public void run() {
        List<InPlayRequest> currentLiveEvents = Collections.emptyList();
        try {
            Instant now = Instant.now();
            currentLiveEvents = pendingGames(now);
            if (!currentLiveEvents.isEmpty()) {
                List<Map<String, String>> partitionedRequests = partitionRequest(currentLiveEvents);
                partitionedRequests.forEach(requestParams -> {
                    List<InPlayResponse> inPlayResponses = betfairInplayClient.getScore(requestParams);
                    if (!isEmpty(inPlayResponses)) {
                        inPlayResponses.forEach(inPlayResponse -> {
                            responses.put(inPlayResponse.getEventId(), inPlayResponse);
                            if (useSnapshotTime) {
                                snapshotKickOffTimes(inPlayResponse, now);
                            }
                            log.debug("inPlay-{} cache snapKickOff={} {}", inPlayResponse.getEventId(), snapKickOffTimes.get(inPlayResponse.getEventId()), inPlayResponse);
                            boolean isEndOfGame = isEndOfGame(inPlayResponse);
                            log.debug("eventId={} isEndOfGame={} saveToBigQuery={}", inPlayResponse.getEventId(), isEndOfGame, saveToBigQuery);
                            if (isEndOfGame) {
                                completedGames.putIfAbsent(inPlayResponse.getEventId(), true);
                                if (saveToBigQuery) {
                                    inPlayDataSaver.execute(() -> {
                                        try {
                                            log.info("Saving inPlayResponse eventId={} {}", inPlayResponse.getEventId(), inPlayResponse);
                                            save(inPlayResponse);
                                        } catch (Throwable e) {
                                            log.error("Error while trying to save betfair in-play record for eventId={}", inPlayResponse.getEventId());
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }
        } catch (RuntimeException e) {
            log.error("InPlayRequestTask error occurred getting in-play data for eventIds={}",
                    currentLiveEvents.stream().map(inPlayRequest -> inPlayRequest.getEventId()).collect(Collectors.toList()), e);
        }
    }

    private void snapshotKickOffTimes(InPlayResponse inplayResponse, Instant now) {
        if (!snapKickOffTimes.containsKey(inplayResponse.getEventId())) {
            List<UpdateDetail> updateDetails = inplayResponse.getUpdateDetails();
            if (!isEmpty(updateDetails)) {
                Optional<UpdateDetail> kickOffDetail = updateDetails.stream().filter(updateDetail -> KICK_OFF.equals(updateDetail.getUpdateType())).findFirst();
                if (kickOffDetail.isPresent()) {
                    Instant kickOffTime = Instant.EPOCH.equals(kickOffDetail.get().getUpdateTime()) ? now : kickOffDetail.get().getUpdateTime();
                    snapKickOffTimes.put(inplayResponse.getEventId(), KickOffTimes.builder().kickOff(kickOffTime).build());
                }
            }
        }
        KickOffTimes kickOffTimes = snapKickOffTimes.get(inplayResponse.getEventId());
        if (kickOffTimes != null && kickOffTimes.getKickOff() != null && kickOffTimes.getSecondHalfKickOff() == null) {
            List<UpdateDetail> updateDetails = inplayResponse.getUpdateDetails();
            if (!isEmpty(updateDetails)) {
                Optional<UpdateDetail> secondHalfStartDetail = updateDetails.stream().filter(updateDetail -> SECOND_HALF_KICK_OFF.equals(updateDetail.getUpdateType())).findFirst();
                if (secondHalfStartDetail.isPresent()) {
                    Instant secondHalfKickOff = Instant.EPOCH.equals(secondHalfStartDetail.get().getUpdateTime()) ? now : secondHalfStartDetail.get().getUpdateTime();
                    snapKickOffTimes.put(inplayResponse.getEventId(), kickOffTimes.toBuilder().secondHalfKickOff(secondHalfKickOff).build());
                }
            }
        }
    }

    private List<Map<String, String>> partitionRequest(List<InPlayRequest> currentLiveEvents) {
        Map<String, String> requestParams = new HashMap<>(stdParams);
        requestParams.put(EVENT_IDS, join(currentLiveEvents, ","));
        return Arrays.asList(requestParams);
    }

    private List<InPlayRequest> pendingGames(Instant now) {
        List<InPlayRequest> currentLiveEvents = requests.values().stream()
                .filter(inPlayRequest -> inPlayRequest.getStartTime().isBefore(now) || inPlayRequest.getStartTime().equals(now))
                .collect(Collectors.toList());
        return currentLiveEvents.stream()
                .filter(inPlayRequest -> !completedGames.containsKey(inPlayRequest.getEventId()))
                .collect(Collectors.toList());
    }

    private void save(InPlayResponse inplayResponse) {
        List<UpdateDetail> updateDetails = inplayResponse.getUpdateDetails();
        updateDetails.sort(Comparator.comparing(UpdateDetail::getMatchTime));
        List<UpdateDetail> toSave = new ArrayList<>(updateDetails);
        Optional<UpdateDetail> epochUpdateTime = updateDetails.stream().filter(updateDetail -> updateDetail.getUpdateTime().getEpochSecond() == 0).findFirst();
        if (epochUpdateTime.isPresent()) {
            log.info("Fixing EPOCH datetime for eventId={}", inplayResponse.getEventId());
            String eventId = String.valueOf(inplayResponse.getEventId());
            Map<String, Instant> kickOffTimes = kickOffTimes(Arrays.asList(eventId));
            if (!isEmpty(kickOffTimes)) {
                toSave.clear();
                Instant kickOffTime = kickOffTimes.get(eventId);
                log.info("Using kickOffTime={} for eventId={}", kickOffTime, eventId);
                boolean inSecondHalf = false;
                Instant updateTime;
                for (UpdateDetail updateDetail : updateDetails) {
                    if (SECOND_HALF_KICK_OFF.equals(updateDetail.getUpdateType())) {
                        inSecondHalf = true;
                    }
                    if (inSecondHalf) {
                        updateTime = kickOffTime.plus(updateDetail.getMatchTime() + 15, ChronoUnit.MINUTES);
                    } else {
                        updateTime = kickOffTime.plus(updateDetail.getMatchTime(), ChronoUnit.MINUTES);
                    }
                    UpdateDetail toSaveDetail = new UpdateDetail(updateTime,
                            updateDetail.getTeam(),
                            updateDetail.getTeamName(),
                            updateDetail.getMatchTime(),
                            updateDetail.getElapsedRegularTime(),
                            updateDetail.getElapsedAddedTime(),
                            updateDetail.getType(),
                            updateDetail.getUpdateType());
                    toSave.add(toSaveDetail);
                }
            } else {
                log.info("kickOffTimes empty for eventId={}", inplayResponse.getEventId());
            }
        }
        List<SoccerInplayRecord> rows = toSoccerInplayRecords(inplayResponse.getEventId(), toSave);
        bigQueryService.insertRows(BETSTORE_DATASET, TABLE, rows.stream().map(soccerInplayRecord -> soccerInplayRecord.toString()).collect(Collectors.toList()));
        Optional<UpdateDetail> secondHalfEnd = updateDetails.stream().filter(updateDetail -> SECOND_HALF_END.equals(updateDetail.getUpdateType())).findFirst();
        if (!secondHalfEnd.isPresent()) {
            log.info("Loading missing secondHalfEnd for eventId={}", inplayResponse.getEventId());
            String sql = String.format("select unix_millis(max(publishTime)) as publishTime, eventId from `betstore.betfair_historical_data` where eventId = '%s' and asbSelectionId in ('1', '2', '3') and status = 'OPEN' and inplay is true and DATE(publishTime) = current_date() group by eventId", inplayResponse.getEventId());
            log.info("sql={}", sql);
            try {
                List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
                if (!isEmpty(resultSet)) {
                    Optional<Object> publishTimeResult = resultSet.iterator().next().get("publishTime");
                    if (publishTimeResult.isPresent()) {
                        Instant secondHalfEndUpdateTime = Instant.ofEpochMilli(Long.valueOf(publishTimeResult.get().toString()));
                        UpdateDetail latestUpateDetail = toSave.get(toSave.size() - 1);
                        int secondHalfEndMatchTime;
                        if (latestUpateDetail.getUpdateTime().getEpochSecond() > 0) {
                            secondHalfEndMatchTime = latestUpateDetail.getMatchTime() + (int) Duration.between(latestUpateDetail.getUpdateTime(), secondHalfEndUpdateTime).toMinutes();
                        } else {
                            if (latestUpateDetail.getMatchTime() < 90) {
                                secondHalfEndMatchTime = 90;
                            } else {
                                secondHalfEndMatchTime = latestUpateDetail.getMatchTime() + 1;
                            }
                        }
                        List<String> scores = getScoreHistory(updateDetails);
                        SoccerInplayRecord secondHalfEndRecord = SoccerInplayRecord.builder()
                                .updateTime(secondHalfEndUpdateTime)
                                .eventId(inplayResponse.getEventId())
                                .updateType(SECOND_HALF_END)
                                .matchTime(secondHalfEndMatchTime)
                                .score(scores.get(scores.size() - 1))
                                .build();
                        bigQueryService.insertRow(BETSTORE_DATASET, TABLE, secondHalfEndRecord.toString());
                        log.info("Added missing SecondHalfEnd eventId={} secondHalfEndRecord={}", inplayResponse.getEventId(), secondHalfEndRecord);
                    }
                }
            } catch (RuntimeException | InterruptedException e) {
                log.error("Error occurred trying to load missing SecondHalfEnd record for eventId={}", inplayResponse.getEventId(), e);
            }
        }
    }

    private Map<String, Instant> kickOffTimes(List<String> eventIds) {
        Map<String, Instant> result = new HashMap<>();
        if (!isEmpty(eventIds)) {
            log.info("Begin kickOffTimes for {} eventIds ", eventIds.size());
            for (List<String> partitionedEventId : partitionBasedOnSize(eventIds, 100)) {
                String sql = createWithSql(partitionedEventId.stream().map(eventId -> String.format(KICK_OFF_TIME_SQL, eventId, eventId, eventId)).collect(Collectors.joining(" UNION ALL ")));
                log.info("sql={}", sql);
                try {
                    List<Map<String, Optional<Object>>> rows = bigQueryService.performQuery(sql);
                    log.info("End sql returned {} rows", rows.size());
                    for (Map<String, Optional<Object>> row : rows) {
                        Optional<Object> eventId = row.get("eventId");
                        Optional<Object> inPlayStart = row.get("inPlayStart");
                        Optional<Object> preLiveEnd = row.get("preLiveEnd");
                        if (eventId.isPresent() && inPlayStart.isPresent() && preLiveEnd.isPresent()) {
                            Long inPlayStartValue = Long.valueOf((String) inPlayStart.get());
                            Long preLiveEndValue = Long.valueOf((String) preLiveEnd.get());
                            long avg = (inPlayStartValue + preLiveEndValue) / 2;
                            result.putIfAbsent((String) (eventId.get()), Instant.ofEpochMilli(avg));
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("Error processing sql={}", sql);
                }
            }
            log.info("End kickOffTimes for {} eventIds ", result.size());
        }
        return result;
    }

    private String createWithSql(String selectQuery) {
        return String.format("WITH kickOff_report AS (%s) SELECT *  FROM `kickOff_report`", selectQuery);
    }


    private String join(List<InPlayRequest> inPlayRequests, String delimiter) {
        return inPlayRequests.stream()
                .map(InPlayRequest::getEventId)
                .map(eventId -> String.valueOf(eventId)).collect(joining(delimiter));
    }

    private List<SoccerInplayRecord> toSoccerInplayRecords(int eventId, List<UpdateDetail> updateDetails) {
        List<SoccerInplayRecord> records = new ArrayList<>();
        List<String> scores = getScoreHistory(updateDetails);
        for (int i = 0; i < updateDetails.size(); i++) {
            UpdateDetail updateDetail = updateDetails.get(i);
            SoccerInplayRecord record = SoccerInplayRecord.builder()
                    .updateTime(updateDetail.getUpdateTime())
                    .eventId(eventId)
                    .matchTime(updateDetail.getMatchTime())
                    .team(updateDetail.getTeam())
                    .updateType(updateDetail.getUpdateType())
                    .score(scores.get(i)).build();
            records.add(record);
        }
        return records;
    }
}
