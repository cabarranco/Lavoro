package com.asb.analytics;

import com.asb.analytics.api.betfair.betting.BetfairBetting;
import com.asb.analytics.api.mercurius.Cerberus;
import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.controllers.EventsController;
import com.asb.analytics.controllers.MarketsController;
import com.asb.analytics.domain.betfair.Event;
import com.asb.analytics.domain.betfair.EventResponse;
import com.asb.analytics.domain.betfair.MarketCatalogue;
import com.asb.analytics.domain.mercurius.FairOdd;
import com.asb.analytics.domain.mercurius.Prediction;
import com.asb.analytics.logs.Logger;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class FairOdds {

    private static final String pattern = "yyyy-MM-dd'T'HH:mm";
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private static final DateFormat df = new SimpleDateFormat(pattern);
    private static final DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static MongoDatabase database;

    private Gson gson = new GsonBuilder().create();
    private final List<String> eventIds;
    private final String sessionToken;
    private final BigQuery bigQuery;

    private List<Float> otherHomeWin = new ArrayList<>();
    private List<Float> otherAwayWin = new ArrayList<>();
    private List<Float> otherDraw = new ArrayList<>();

    static {
        database = new MongoConnector().connect();
    }

    FairOdds(List<String> eventIds, String sessionToken, BigQuery bigQuery) {
        this.eventIds = eventIds;
        this.sessionToken = sessionToken;
        this.bigQuery = bigQuery;
    }

    Boolean collect() {

        ExecuteShellCommand com = new ExecuteShellCommand();
        String token = com.executeCommand("gcloud auth print-access-token");

        Logger.log().info("Sync with mercurius started...");

        List<Row> values = new ArrayList<>();

        List<FairOdd> fairOdds = MongoUtils
                                        .query(database)
                                        .getFairOdds();

        // TODO: non stoppare il ciclo, controllare quali sono stati presi e quali no e provare a prendere i mancanti.
        if (fairOdds.size() > 0) return true;

        for (String eventId : eventIds) {

            Logger.planeLog().info("--------------------------------");

            HashMap<String, Object> filters = new HashMap<>();
            filters.put("eventIds", Collections.singleton(eventId));

            List<String> competitionIds = BetfairBetting.init(sessionToken)
                    .getCompetitionIds(filters);

            HashMap<String, Long> betfairTeamIds = getBetfairTeamIds(Integer.valueOf(eventId));

            HashMap<String, Long> cerberusTeamIds = getCerberusTeamId(betfairTeamIds);

            Long cerberusCompetitionId = getCerberusCompetitionId(Integer.valueOf(competitionIds.get(0)));

            // TODO: rimuovere questa parte di codice ed utilizzare gli eventi presi nel main
            List<EventResponse> events = new ArrayList<>();
            try {
                events = new EventsController(sessionToken, eventId).getSoccerEvents();
            } catch (Exception e) {
                Logger.planeLog().error("RETRIEVING EVENTS ERROR");
                Logger.log().error(e.getMessage());
            }

            List<Prediction> predictions = new ArrayList<>();

            if (cerberusTeamIds.get("home") != null && cerberusTeamIds.get("away") != null) {
                predictions = new Cerberus().getPredictions(
                        cerberusTeamIds.get("home"),
                        cerberusTeamIds.get("away"),
                        df.format(events.get(0).getEvent().getOpenDate()),
                        cerberusCompetitionId
                );
            } else {
                Logger.log().error("Cerberus team ids not found");
            }

            values.addAll(getFairOdds(predictions, events.get(0).getEvent(), Integer.valueOf(eventId)));
        }

        if (values.size() > 0) {

            List<Document> rows = new ArrayList<>();

            for (Row row : values) {
                Document document = ((FairOdd) row.getJson()).toDocument();
                rows.add(document);
            }

            if (rows.size() > 0) {
                MongoUtils
                        .query(database)
                        .saveEventUpdate(rows, "event_fair_prices");
            }

            BigQueryServices.oddsSizes()
                    .insertBigQuery(gson, values, token, "event_fair_prices");
        }

        return null;
    }

    private List<Row> getFairOdds(List<Prediction> predictions, Event event, Integer eventId) {
        List<Row> values = new ArrayList<>();

        Logger.log().info("Calculating fair odds...");

        if (predictions.isEmpty()) {
            values.add(
                    new Row<>(
                            new FairOdd(
                                    eventId,
                                    df2.format(event.getOpenDate()),
                                    false,
                                    null,
                                    null
                            )
                    )
            );

            return values;
        }

        Prediction prediction = predictions.get(0);

        values.add(
                new Row<>(
                        new FairOdd(
                                eventId,
                                df2.format(event.getOpenDate()),
                                true,
                                    "Home",
                                Float.valueOf(decimalFormat.format(1 / (prediction.getHome() / 100)))
                        )
                )
        );

        values.add(
                new Row<>(
                        new FairOdd(
                                eventId,
                                df2.format(event.getOpenDate()),
                                true,
                                "Draw",
                                Float.valueOf(decimalFormat.format(1 / (prediction.getDraw() / 100)))
                        )
                )
        );

        values.add(
                new Row<>(
                        new FairOdd(
                                eventId,
                                df2.format(event.getOpenDate()),
                                true,
                                "Away",
                                Float.valueOf(decimalFormat.format(1 / (prediction.getAway() / 100)))
                        )
                )
        );

        prediction.getTotalGoals().forEach( (name, odd) -> {
            String fairName = getBackFairPriceName(name);
            Float fairOdd = getBackFairPriceValue(fairName, odd);

            if (fairOdd != null) {
                values.add(
                        new Row<>(
                                new FairOdd(
                                        eventId,
                                        df2.format(event.getOpenDate()),
                                        true,
                                        getBackFairPriceName(name),
                                        getBackFairPriceValue(name, odd)
                                )
                        )
                );
            }
        });

        if (!otherAwayWin.isEmpty()) {
            values.add(
                    new Row<>(
                            new FairOdd(
                                    eventId,
                                    df2.format(event.getOpenDate()),
                                    true,
                                    "Any Other Away Win",
                                    Float.valueOf(decimalFormat.format(1 / sum(otherAwayWin)))

                            )
                    )
            );
        }

        if (!otherHomeWin.isEmpty()) {
            values.add(
                    new Row<>(
                            new FairOdd(
                                    eventId,
                                    df2.format(event.getOpenDate()),
                                    true,
                                    "Any Other home Win",
                                    Float.valueOf(decimalFormat.format(1 / sum(otherHomeWin)))
                            )
                    )
            );
        }

        if (!otherDraw.isEmpty()) {
            values.add(
                    new Row<>(
                            new FairOdd(
                                    eventId,
                                    df2.format(event.getOpenDate()),
                                    true,
                                    "Any Other Draw",
                                    Float.valueOf(decimalFormat.format(1 / sum(otherDraw)))
                            )
                    )
            );
        }

        return values;
    }

    private String getBackFairPriceName(String runnerName) {

        Logger.log().info("Retrieving back fair price name...");

        Integer home = Integer.valueOf(runnerName.split("-")[0]);
        Integer away = Integer.valueOf(runnerName.split("-")[1]);

        if (home <= 3 && away <= 3) return runnerName;

        if (home > away) return "Any Other Home Win";

        if (away > home) return "Any Other Away Win";

        if (away.equals(home)) return "Any Other Draw";

        return runnerName;
    }

    private Float getBackFairPriceValue(String fairName, Float odd) {

        Logger.log().info("Retrieving back fair price value...");

        if ("Any Other Draw".equalsIgnoreCase(fairName)) {
            otherDraw.add(odd);
            return null;
        }

        if ("Any Other Home Win".equalsIgnoreCase(fairName)) {
            otherHomeWin.add(odd);
            return null;
        }

        if ("Any Other Away Win".equalsIgnoreCase(fairName)) {
            otherAwayWin.add(odd);
            return null;
        }

        return Float.valueOf(decimalFormat.format(1/odd));
    }

    private HashMap<String, Long> getBetfairTeamIds(Integer eventId) {

        Logger.log().info("Retrieving betfair team ids for event: " + eventId);

        List<MarketCatalogue> catalogues = null;
        try {
            catalogues = new MarketsController(sessionToken)
                    .getMatchOddsMarketCatalogues(eventId);
        } catch (Exception e) {
            Logger.planeLog().error("RETRIEVING MATCH ODDS MARKET CATALOGUE ERROR");
            Logger.log().error(e.getMessage());
        }

        HashMap<String, Long> ids = new HashMap<>();

        if (catalogues.size() == 0) {
            ids.put("home", null);
            ids.put("away", null);

            return ids;
        }

        ids.put("home", catalogues.get(0).getRunners().get(0).getSelectionId());
        ids.put("away", catalogues.get(0).getRunners().get(1).getSelectionId());

        String homeName = catalogues.get(0).getRunners().get(0).getRunnerName();
        String awayName = catalogues.get(0).getRunners().get(1).getRunnerName();

        Logger.log().info("Home: " + homeName + " " + ids.get("home"));
        Logger.log().info("Away: " + awayName + " " + ids.get("away"));

        return ids;
    }

    private HashMap<String, Long> getCerberusTeamId(HashMap<String, Long> betfairIds) {

        Logger.log().info("Retrieving mercurius team ids...");

        Long home = betfairIds.get("home");
        Long away = betfairIds.get("away");

        HashMap<String, Long> ids = new HashMap<>();

        if (home == null || away == null) {
            ids.put("home", null);
            ids.put("away", null);
            return ids;
        }

        String query = String.format("SELECT betfairTeamId, cerberusTeamId FROM `asbanalytics.static_tables.team_dictionary` where betfairTeamId = %d or betfairTeamId = %d", home, away);
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

        try {

            for (FieldValueList row : bigQuery.query(queryConfig).iterateAll()) {
                if (home.equals(row.get(0).getLongValue())) {
                    ids.put("home", row.get(1).getLongValue());
                } else if (away.equals(row.get(0).getLongValue())) {
                    ids.put("away", row.get(1).getLongValue());
                }
            }

        } catch (InterruptedException e) {
            Logger.log().error("Mercurius team ids bigquery error");
            Logger.planeLog().error(e.getMessage());
        }

        Logger.log().info("Home: " + ids.get("home"));
        Logger.log().info("Away: " + ids.get("away"));

        return ids;
    }

    private Long getCerberusCompetitionId(Integer betfairCompetitionId) {

        Logger.log().info("Retrieving mercurius competition id from betfair competition: " + betfairCompetitionId);

        String query = String.format("SELECT cerberusCompetitionId FROM `asbanalytics.static_tables.competition_dictionary` where betfairCompetitionId=%d", betfairCompetitionId);
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        long id = 0L;

        try {

            for (FieldValueList row : bigQuery.query(queryConfig).iterateAll()) {
                id = row.get(0).getLongValue();
            }

        } catch (InterruptedException e) {
            Logger.log().error("Mercurius competition id bigquery error");
            Logger.planeLog().error(e.getMessage());
        }

        Logger.log().info("Mercurius competition id ");

        return id;

    }

    private float sum(List<Float> list) {
        float sum = 0f;
        for (Float i: list) {
            if(i != null) sum = sum + i;
        }
        return sum;
    }
}
