package com.asb.analytics;

import com.asb.analytics.logs.Logger;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.asb.analytics.util.CalendarUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.mongodb.client.MongoDatabase;

import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class MainEventUpdates {

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

        // set gcloud token first time
        AtomicReference<String> gcloudToken = new AtomicReference<>(new TokenRefresher().call());

        List<String> allEventsIds = MongoUtils.query(database).getAllEventsIds();

        ScheduledExecutorService execService = Executors.newScheduledThreadPool(3);

        // Schedule a task to run every 4 minutes with 0 minute initial delay to get alla the events updates
        ScheduledFuture<?> eventUpdatesThread = execService.scheduleAtFixedRate(() ->
                new EventUpdates(allEventsIds, bigquery, gcloudToken.get()).call(), 4L, 2L, TimeUnit.MINUTES);

        // Schedule a task to run every 10 minutes with 1 minute initial delay to refresh gcloud token
        ScheduledFuture<?> tokenThread = execService.scheduleAtFixedRate(() -> gcloudToken.set(new TokenRefresher().call()), 10L, 15L, TimeUnit.MINUTES);

        try {
            eventUpdatesThread.get();
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
