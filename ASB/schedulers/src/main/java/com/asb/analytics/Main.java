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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class Main {

    private static MongoDatabase database;
    private static BigQuery bigquery;

    static {
        Logger.log().mainStart();
        try {
//            String path = Main.class.getClassLoader().getResource("asbanalytics-981daed66622.json").getFile();
            String path = "./asbanalytics-981daed66622.json";
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

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Boolean>> taskList = new ArrayList<>();
        HashMap<String, Double> lastSavedRunners = new HashMap<>();
        List<Integer> eventIds = getEventIds();

        Logger.log().info("Scheduled event ids found: " + eventIds.size());

        // set gcloud token first time
        AtomicReference<String> gcloudToken = new AtomicReference<>(new TokenRefresher().call());

        Logger.log().info("Logging in betfair dev...");
        String sessionToken = BetfairAuth.login();

        if (sessionToken.isEmpty()) Logger.log().error("BetFair login failed");
        else Logger.log().info("Session token: " + sessionToken);

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

        Logger.log().info("Events retrieved from Betfair: " + events.size());

        new EventUpdates(allEventsIds, bigquery, gcloudToken.get()).call();

        for (Integer eventId : eventIds) {
            taskList.add(new AnalyticsTick(sessionToken, Integer.toString(eventId), gcloudToken.get()));
        }

        ScheduledExecutorService execService = Executors.newScheduledThreadPool(3);

        // Schedule a task to run every 4 minutes with 0 minute initial delay to get alla the events updates
        ScheduledFuture<?> eventUpdatesThread = execService.scheduleAtFixedRate(() ->
                new EventUpdates(allEventsIds, bigquery, gcloudToken.get()).call(), 4L, 2L, TimeUnit.MINUTES);

        ScheduledFuture<?> anayticsThread = execService.scheduleAtFixedRate(() -> new Analytics(sessionToken, eventIds, eventResponses, lastSavedRunners, gcloudToken.get()).call(), 1L, 30L, TimeUnit.SECONDS);

        // Schedule a task to run every 10 minutes with 1 minute initial delay to refresh gcloud token
        ScheduledFuture<?> tokenThread = execService.scheduleAtFixedRate(() -> gcloudToken.set(new TokenRefresher().call()), 10L, 15L, TimeUnit.MINUTES);

        try {
            eventUpdatesThread.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        try {
            anayticsThread.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        try {
            tokenThread.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        try {
            List<Future<Boolean>> futures = executor.invokeAll(taskList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
