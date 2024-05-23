package com.asb.analytics;

import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CleanFakeGoals {

    private static GoogleCredentials credentials;
    private static MongoDatabase database;

    static {
        try {
            String path = Main.class.getClassLoader().getResource("asbanalytics-981daed66622.json").getFile();
//            String path = "./asbanalytics-981daed66622.json";
            credentials = GoogleCredentials.fromStream(new FileInputStream(path));
            database = new MongoConnector().connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static BigQuery bigquery = BigQueryOptions.newBuilder()
            .setCredentials(credentials)
            .build().getService();



    public static void main(String[] args) {

        List<Integer> eventIds = getEventIds();

        for (Integer eventId : eventIds) {
            List<String> wrongUpdateTime;

            wrongUpdateTime = new ArrayList<>();
            String queryUpdates = String.format("SELECT eventScore, CAST(updateTime as DATETIME) FROM `asbanalytics.betstore.event_updates` where eventId = %s order by updateTime ASC", eventId);
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryUpdates).build();

            int lastHome = 0;
            int lastAway = 0;
            String lastUpdateTime = "";

            try {
                for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
                    String score = row.get(0).getStringValue();
                    String updateTime = row.get(1).getStringValue();

                    int home = Integer.valueOf(score.split("-")[0].trim());
                    int away = Integer.valueOf(score.split("-")[1].trim());

                    if ((home > 0 || away > 0) && (lastHome > home || lastAway > away)) {
                        wrongUpdateTime.add(lastUpdateTime);
                    }

                    lastHome = home;
                    lastAway = away;
                    lastUpdateTime = updateTime;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            wrongUpdateTime.forEach(time -> removeLine(time, String.valueOf(eventId)));
        }

    }

    private static void removeLine(String updateTime, String eventId) {

        try {
            String queryDelete = String.format("UPDATE `asbanalytics.betstore.event_updates` SET annulled = TRUE where eventId = %s and updateTime = \"%s\"", eventId, updateTime);
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryDelete).build();

            bigquery.query(queryConfig);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<Integer> getEventIds() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Date start = calendar.getTime();

        calendar = Calendar.getInstance();
        // remove three hours from now to get all the event with start date time more than three hours ago. We consider
        // two hours for the match +1 hour before Google BigQeury can store the lines from the buffer.
        calendar.add(Calendar.HOUR, -3);
        Date end = calendar.getTime();

        return MongoUtils.query(database)
                .collection(MongoConnector.COLLECTION_EVENTS)
                .eventIdsInDateRange(start, end);
    }
}
