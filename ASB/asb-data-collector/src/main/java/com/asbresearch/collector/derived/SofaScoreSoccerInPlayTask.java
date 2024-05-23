package com.asbresearch.collector.derived;

import com.asbresearch.collector.model.*;
import com.asbresearch.collector.model.SofaScoreSoccerInPlayRecord.SofaScoreSoccerInPlayRecordBuilder;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.model.SofaScoreIncident;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.asbresearch.collector.model.SofaScoreSoccerInPlayRecord.TABLE;
import static com.asbresearch.common.BigQueryUtil.BETSTORE_DATASET;
import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
public class SofaScoreSoccerInPlayTask implements Runnable {
    public static final String SECOND_HALF_KICK_OFF = "SecondHalfKickOff";
    public static final String FIRST_HALF_END = "FirstHalfEnd";
    public static final String SECOND_HALF_END = "SecondHalfEnd";
    public static final String INJURY_TIME = "injuryTime";
    public static final String KICK_OFF = "KickOff";
    public static final String GOAL = "goal";
    public static final String START_SCORE = "0-0";
    public static final String PERIOD = "period";
    public static final String CARD = "card";
    public static final String GOAL_UPDATE_TYPE = "Goal";
    public static final String HOME = "home";
    public static final String AWAY = "away";
    public static final String YELLOW_CARD = "YellowCard";
    public static final String RED_CARD = "RedCard";
    public static final String YELLOW = "yellow";
    public static final String SUBSTITUTION = "substitution";

    private final BigQueryService bigQueryService;
    private final Map<String, Boolean> completed;
    private final String eventId;
    private final String SQL = "SELECT UNIX_SECONDS(E.startTime) as startTime, I.eventId, I.time, I.incidentClass, I.incidentType, I.json " +
            "FROM `betstore.sofascore_event_incidents` I " +
            "JOIN `betstore.sofascore_events` E " +
            "ON E.id = I.eventId " +
            "where I.eventId = '%s' " +
            "order by I.index desc";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SofaScoreSoccerInPlayTask(BigQueryService bigQueryService,
                                     Map<String, Boolean> completed,
                                     String eventId) {
        this.bigQueryService = bigQueryService;
        this.completed = completed;
        this.eventId = eventId;
    }

    @Override
    public void run() {
        if (!completed.containsKey(eventId)) {
            log.info("Loading eventId={}", eventId);
            String sql = String.format(SQL, eventId);
            log.info("sql={}", sql);
            try {
                List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
                if (!resultSet.isEmpty()) {
                    List<SofaScoreIncident> incidents = resultSet.stream()
                            .map(this::toSofaScoreIncident)
                            .collect(Collectors.toList());
                    incidents = incidents.stream()
                            .filter(this::filterIncident)
                            .collect(Collectors.toList());
                    Instant startTime = Instant.ofEpochSecond(Long.parseLong(resultSet.iterator().next().get("startTime").get().toString()));
                    List<SofaScoreSoccerInPlayRecord> sofaScoreSoccerInPlayRecords = toSoccerInPlayRecords(startTime, incidents);
                    if (!sofaScoreSoccerInPlayRecords.isEmpty()) {
                        List<String> inPlayRecords = sofaScoreSoccerInPlayRecords.stream()
                                .map(sofaScoreSoccerInPlayRecord -> sofaScoreSoccerInPlayRecord.toCsv())
                                .collect(Collectors.toList());
                        bigQueryService.insertRows(BETSTORE_DATASET, TABLE, inPlayRecords);
                        completed.putIfAbsent(eventId, Boolean.TRUE);
                    }
                }
            } catch (InterruptedException e) {
                log.error("Error loading SofaScore incidents for eventId={} sql={}", eventId, sql, e);
            }
        }
    }

    private boolean filterIncident(SofaScoreIncident incident) {
        switch (incident.getIncidentType()) {
            case GOAL:
            case PERIOD:
            case CARD:
            case INJURY_TIME:
            case SUBSTITUTION:
                return true;
        }
        return false;
    }

    private SofaScoreIncident toSofaScoreIncident(Map<String, Optional<Object>> row) {
        int time = Integer.valueOf(row.get("time").orElse("-1").toString());
        String incidentClass = row.get("incidentClass").orElse("").toString();
        String incidentType = row.get("incidentType").orElse("").toString();
        SofaScoreIncident incident = new SofaScoreIncident(null, time, incidentClass, incidentType);
        incident.setEventId(row.get("eventId").orElse("").toString());
        incident.setJson(row.get("json").orElse("").toString());
        return incident;
    }

    @SneakyThrows
    private List<SofaScoreSoccerInPlayRecord> toSoccerInPlayRecords(Instant startTime, List<SofaScoreIncident> incidents) {
        List<SofaScoreSoccerInPlayRecord> inPlayRecords = createRecordStartWithKickOff(startTime);
        Map<Integer, SofaScoreInjuryTime> injuryTimes = getInjuryTimes(incidents);
        for (int i = 0; i < incidents.size(); i++) {
            SofaScoreIncident incident = incidents.get(i);
            SofaScoreSoccerInPlayRecordBuilder builder = SofaScoreSoccerInPlayRecord.builder().eventId(eventId);
            switch (incident.getIncidentType()) {
                case PERIOD:
                    SofaScorePeriod period = objectMapper.readValue(incident.getJson(), SofaScorePeriod.class);
                    int extraTime = 0;
                    if (period.getTime() == 45 || "HT".equals(period.getText())) {
                        builder.updateType(FIRST_HALF_END);
                        if (injuryTimes.containsKey(45)) {
                            extraTime = injuryTimes.get(45).getLength();
                        }
                        builder.matchTime(period.getTime() + extraTime);
                    } else {
                        createSecondHalfKickOff(inPlayRecords);
                        builder.updateType(SECOND_HALF_END);
                        if (injuryTimes.containsKey(90)) {
                            extraTime += injuryTimes.get(90).getLength();
                        }
                        builder.matchTime(period.getTime() + extraTime);
                        if (injuryTimes.containsKey(45)) {
                            extraTime += injuryTimes.get(45).getLength();
                        }
                        extraTime += 15;
                    }
                    builder.updateTime(startTime.plus(period.getTime() + extraTime, MINUTES));
                    builder.score(String.format("%s-%s", period.getHomeScore(), period.getAwayScore()));
                    inPlayRecords.add(builder.build());
                    break;
                case GOAL:
                    SofaScoreGoal goal = objectMapper.readValue(incident.getJson(), SofaScoreGoal.class);
                    builder.matchTime(incident.getTime() + goal.getAddedTime());
                    builder.updateType(GOAL_UPDATE_TYPE);
                    builder.team(goal.isHome() ? HOME : AWAY);
                    if (incident.getTime() > 45) {
                        int firstHalfExtraTime = injuryTimes.get(45) != null ? injuryTimes.get(45).getLength() : 0;
                        builder.updateTime(startTime.plus(firstHalfExtraTime + 15 + incident.getTime() + goal.getAddedTime(), MINUTES));
                    } else {
                        builder.updateTime(startTime.plus(incident.getTime() + goal.getAddedTime(), MINUTES));
                    }
                    builder.score(String.format("%s-%s", goal.getHomeScore(), goal.getAwayScore()));
                    inPlayRecords.add(builder.build());
                    break;
                case CARD:
                    SofaScoreCard card = objectMapper.readValue(incident.getJson(), SofaScoreCard.class);
                    builder.matchTime(incident.getTime() + card.getAddedTime());
                    if (YELLOW.equals(card.getIncidentClass())) {
                        builder.updateType(YELLOW_CARD);
                    } else {
                        builder.updateType(RED_CARD);
                    }
                    builder.team(card.isHome() ? HOME : AWAY);
                    if (incident.getTime() > 45) {
                        int firstHalfExtraTime = injuryTimes.get(45) != null ? injuryTimes.get(45).getLength() : 0;
                        builder.updateTime(startTime.plus(firstHalfExtraTime + 15 + incident.getTime() + card.getAddedTime(), MINUTES));
                    } else {
                        builder.updateTime(startTime.plus(incident.getTime() + card.getAddedTime(), MINUTES));
                    }
                    builder.score(inPlayRecords.get(inPlayRecords.size() - 1).getScore());
                    inPlayRecords.add(builder.build());
                    break;
                case SUBSTITUTION:
                    SofaScoreSubstitution sub = objectMapper.readValue(incident.getJson(), SofaScoreSubstitution.class);
                    builder.matchTime(incident.getTime() + sub.getAddedTime());
                    builder.updateType("Substitution");
                    builder.team(sub.isHome() ? HOME : AWAY);
                    if (incident.getTime() > 45) {
                        int firstHalfExtraTime = injuryTimes.get(45) != null ? injuryTimes.get(45).getLength() : 0;
                        builder.updateTime(startTime.plus(firstHalfExtraTime + 15 + incident.getTime() + sub.getAddedTime(), MINUTES));
                    } else {
                        builder.updateTime(startTime.plus(incident.getTime() + sub.getAddedTime(), MINUTES));
                    }
                    builder.score(inPlayRecords.get(inPlayRecords.size() - 1).getScore());
                    inPlayRecords.add(builder.build());
                    break;
            }
        }
        return inPlayRecords;
    }

    private void createSecondHalfKickOff(List<SofaScoreSoccerInPlayRecord> inPlayRecords) {
        Optional<SofaScoreSoccerInPlayRecord> firstHalfEndRecord = inPlayRecords.stream()
                .filter(item -> FIRST_HALF_END.equals(item.getUpdateType()))
                .findFirst();
        firstHalfEndRecord.ifPresent(firstHalfEnd -> {
            SofaScoreSoccerInPlayRecordBuilder builder = SofaScoreSoccerInPlayRecord.builder()
                    .eventId(eventId)
                    .matchTime(46)
                    .updateType(SECOND_HALF_KICK_OFF);
            builder.updateTime(firstHalfEnd.getUpdateTime().plus(15, MINUTES));
            builder.score(firstHalfEnd.getScore());
            inPlayRecords.add(builder.build());
        });
    }

    @SneakyThrows
    private Map<Integer, SofaScoreInjuryTime> getInjuryTimes(List<SofaScoreIncident> incidents) {
        Map<Integer, SofaScoreInjuryTime> result = new HashMap<>();
        for (SofaScoreIncident incident : incidents) {
            if (incident.getIncidentType().equals(INJURY_TIME)) {
                SofaScoreInjuryTime injuryTime = objectMapper.readValue(incident.getJson(), SofaScoreInjuryTime.class);
                result.putIfAbsent(injuryTime.getTime(), injuryTime);
            }
        }
        return result;
    }

    private List<SofaScoreSoccerInPlayRecord> createRecordStartWithKickOff(Instant startTime) {
        List<SofaScoreSoccerInPlayRecord> inPlayRecords = new ArrayList<>();
        SofaScoreSoccerInPlayRecord kickOffRecord = SofaScoreSoccerInPlayRecord.builder()
                .updateTime(startTime)
                .eventId(eventId)
                .matchTime(1)
                .updateType(KICK_OFF)
                .score(START_SCORE)
                .build();
        inPlayRecords.add(kickOffRecord);
        return inPlayRecords;
    }
}
