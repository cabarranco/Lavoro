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
import com.mongodb.client.MongoDatabase;

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
public class MainAnalytics {

    private static MongoDatabase database;

    static {
        Logger.log().mainStart();
        try {
            database = new MongoConnector().connect();
        } catch (Exception e) {
            Logger.log().fatalError(e.getMessage());
        }
    }

    public static void main(String[] args) {

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

        Logger.log().info("Events retrieved from Betfair: " + events.size());

        ScheduledExecutorService execService = Executors.newScheduledThreadPool(2);

        ScheduledFuture<?> anayticsThread = execService.scheduleAtFixedRate(() -> new Analytics(sessionToken, eventIds, eventResponses, lastSavedRunners, gcloudToken.get()).call(), 1L, 30L, TimeUnit.SECONDS);

        // Schedule a task to run every 10 minutes with 1 minute initial delay to refresh gcloud token
        ScheduledFuture<?> tokenThread = execService.scheduleAtFixedRate(() -> gcloudToken.set(new TokenRefresher().call()), 10L, 15L, TimeUnit.MINUTES);


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
