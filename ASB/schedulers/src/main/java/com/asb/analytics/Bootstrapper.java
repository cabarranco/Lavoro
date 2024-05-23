package com.asb.analytics;

import com.asb.analytics.api.betfair.account.BetfairAuth;
import com.asb.analytics.api.betfair.filters.TimeRange;
import com.asb.analytics.controllers.EventsController;
import com.asb.analytics.domain.betfair.Event;
import com.asb.analytics.domain.betfair.EventResponse;
import com.asb.analytics.logs.Logger;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.asb.analytics.util.CalendarUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class will take care of the event daily followed by the market catalogue and the fair odds
 */
public class Bootstrapper {

    private static MongoDatabase database;
    private static BigQuery bigquery;

    static {
        Logger.log().mainStart();
        try {
            String path = Main.class.getClassLoader().getResource("asbanalytics-981daed66622.json").getFile();
//            String path = "./asbanalytics-981daed66622.json";
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(path));
            database = new MongoConnector().connect();

            bigquery = BigQueryOptions.newBuilder()
                    .setCredentials(credentials)
                    .build().getService();
        } catch (Exception e) {
            Logger.log().fatalError(e.getMessage());
        }
    }

    public static void main(String[] args) {

        Logger.log().info("Bootstrapper starting...");

        // Get betfair session token through the login
        String sessionToken = BetfairAuth.login();

        // Get gcloud session token executing shell command
        String gcloudToken = new TokenRefresher().call();

        if (sessionToken.isEmpty()) Logger.log().error("BetFair login failed");
        if (gcloudToken.isEmpty()) Logger.log().error("GC token failed");

        // Clean daily events collection
        MongoUtils.query(database).cleanAllEventsIds();

        // Get tick events ids
        List<Integer> eventIds = getEventIds();

        EventsController eventsController = new EventsController(sessionToken, TimeRange.today());

        final List<EventResponse> eventResponses = new ArrayList<>();
        try {
            eventResponses.addAll(eventsController.getSoccerEvents());
        } catch (Exception e) {
            Logger.planeLog().error("RETRIEVING MARKET CATALOGUE ERROR");
            Logger.log().error(e.getMessage());
        }

        List<Event> events = eventResponses.stream().map(EventResponse::getEvent).collect(Collectors.toList());
        List<String> allEventsIds = events.stream().map(Event::getId).collect(Collectors.toList());

        // Cast list string in list of org.bson.Document
        List<Document> documents = new ArrayList<>();
        allEventsIds.forEach(eventId -> {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("eventId", eventId);
            documents.add(new Document(hashMap));
        });

        MongoUtils.query(database).addAllEventsIds(documents);

        Logger.log().info("Events retrieved from Betfair: " + events.size());

        Boolean marketCatalogueResult =
                new MarketCatalogueCollector(
                        sessionToken,
                        eventIds,
                        bigquery,
                        eventResponses,
                        allEventsIds,
                        gcloudToken
                ).collect();

        Boolean fairOddsresult = new FairOdds(allEventsIds, sessionToken, bigquery).collect();

        // TODO: handle errors in market catalogue and fair odds

        ExecuteShellCommand com = new ExecuteShellCommand();

//        com.executeCommand("./event-updates.sh");
//        com.executeCommand("./analytics.sh");
//        com.executeCommand("./analytics-tick.sh");
    }

    private static List<Integer> getEventIds() {

        return MongoUtils.query(database)
                .collection(MongoConnector.COLLECTION_EVENTS)
                .eventIdsInDateRange(
                        CalendarUtils.softwareStartDateTime(),
                        CalendarUtils.softwareStopDateTime()
                );
    }
}
