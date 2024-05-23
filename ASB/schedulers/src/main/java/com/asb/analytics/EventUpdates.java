package com.asb.analytics;

import com.asb.analytics.api.betfair.betting.BetfairBetting;
import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.EventUpdate;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.domain.EventUpdateDetails;
import com.asb.analytics.domain.LiveScore;
import com.asb.analytics.exceptions.EventUpdatesNotFoundException;
import com.asb.analytics.logs.Logger;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventUpdates implements Callable<Boolean> {

    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private static MongoDatabase database;
    private final List<String> eventsIds;
    private final Gson gson = new Gson();
    private String gcloudToken;

    private BigQuery bigQuery;

    EventUpdates(
            List<String> eventsIds,
            BigQuery bigquery,
            String gcloudToken
    ) {
        this.eventsIds = eventsIds;
        this.bigQuery = bigquery;
        this.gcloudToken = gcloudToken;
    }

    static {
        database = new MongoConnector().connect();
    }

    @Override
    public Boolean call() {

        Logger.log().info("Starting correct score...");

        Logger.log().info("Retrieving live score events...");
        List<LiveScore> liveScores = BetfairBetting.getLiveScore(eventsIds.toArray(new String[0]));

        Logger.log().info("Live events found: " + liveScores.size());

        Timestamp now = new Timestamp(new Date().getTime());

        List<Integer> terminatedEvents = new ArrayList<>();

        List<Document> updates = new ArrayList<>();

        for (LiveScore liveScore : liveScores) {

            List<EventUpdate> mongoUpdates = new ArrayList<>();
            try {
                mongoUpdates = MongoUtils.query(database).getEventUpdates(liveScore.getEventId());
            } catch (EventUpdatesNotFoundException e) {
                Logger.planeLog().info("Event updates not found with id: " + liveScore.getEventId());
            }

            Logger.planeLog().info("Event: " + liveScore.getEventId());
            Logger.planeLog().info("Update details collected: " + liveScore.getUpdateDetails().size());

            for (EventUpdateDetails updateDetails : liveScore.getUpdateDetails()) {

                Logger.planeLog().info("\t" + updateDetails.getType() + ": " + updateDetails.getTeamName());

                AtomicBoolean found = new AtomicBoolean(false);

                mongoUpdates.forEach(eventUpdate -> {
                    long time1 = updateDetails.getUpdateTime().getTime();
                    long time2 = -1L;

                    int matchTime1 = updateDetails.getMatchTime() == null ? -1 : updateDetails.getMatchTime();
                    int matchTime2 = eventUpdate.getMatchTime() == null ? -1 : eventUpdate.getMatchTime();

                    try {
                        if ("1970-01-01 01:00:00".equalsIgnoreCase(eventUpdate.getUpdateTime())) {
                            time2 = 0;
                        } else {
                            time2 = dateTimeFormat.parse(eventUpdate.getUpdateTime()).getTime();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    if (
                            eventUpdate.getEventId().equals(liveScore.getEventId())
                            && time1 == time2
                            && matchTime1 == matchTime2
                    ) {
                        found.set(true);
                    }
                });

                if (!found.get()) {

                    Integer score = 0;

                    if (updateDetails.getTeam() != null) {
                        score = ("home".equalsIgnoreCase(updateDetails.getTeam()))
                                ? liveScore.getScore().getHome().getScore() : liveScore.getScore().getAway().getScore();
                    }

                    String fullScore = liveScore.getScore().getHome().getScore() + " - " + liveScore.getScore().getAway().getScore();

                    Integer corners = liveScore.getScore().getNumberOfCornersFirstHalf() + liveScore.getScore().getNumberOfCornersSecondHalf();

                    EventUpdate eventUpdate = new EventUpdate(
                            liveScore.getEventId(),
                            updateDetails.getTeam(),
                            updateDetails.getTeamName(),
                            score == null ? 0 : score,
                            liveScore.getScore().getNumberOfYellowCards(),
                            liveScore.getScore().getNumberOfRedCards(),
                            dateTimeFormat.format(updateDetails.getUpdateTime()),
                            updateDetails.getMatchTime(),
                            updateDetails.getUpdateType(),
                            liveScore.getInPlayMatchStatus(),
                            corners,
                            fullScore,
                            dateTimeFormat.format(now)
                    );

                    updates.add(eventUpdate.toDocument());

                    if ("Finished".equalsIgnoreCase(liveScore.getInPlayMatchStatus())) {

                        if (!terminatedEvents.contains(liveScore.getEventId())) {
                            terminatedEvents.add(liveScore.getEventId());
                        }
                    }

                    System.out.println(ANSI_YELLOW + "Event update (" + liveScore.getEventId() + "): " + updateDetails.getUpdateType() + ANSI_RESET);
                }
            }
        }

        if (updates.size() > 0) {

            MongoUtils
                    .query(database)
                    .saveEventUpdate(updates, "events_updates");

            saveOnBigQuery(terminatedEvents);
            updates.clear();
        }

        return true;

    }

    private void saveOnBigQuery(List<Integer> eventIds) {

        if (eventIds.size() == 0) return;

        Logger.log().error("Saving on BQ from mongo");

        List<Row> rows = new ArrayList<>();

        for (Integer eventId : eventIds) {

            String query = String.format("SELECT COUNT(eventId) FROM `asbanalytics.odds_sizes.event_updates` where eventId = %d group by eventId", eventId);
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

            // Print the results.
            try {
                for (FieldValueList row : bigQuery.query(queryConfig).iterateAll()) {
                    if (row.get(0).getLongValue() > 0)
                        break;
                }
            } catch (Exception ignored) {}

            List<EventUpdate> mongoUpdates;
            try {
                mongoUpdates = MongoUtils.query(database).getEventUpdates(eventId);

                mongoUpdates.forEach(update -> rows.add(new Row<>(update)));
            } catch (EventUpdatesNotFoundException e) {
                Logger.planeLog().info("Event updates not found with id: " + eventId);
            }

        }

        Logger.log().error("Rows from mongo on BQ: " + rows.size());

        BigQueryServices.oddsSizes()
                        .insertBigQuery(gson, rows, gcloudToken, "event_updates");
    }
}
