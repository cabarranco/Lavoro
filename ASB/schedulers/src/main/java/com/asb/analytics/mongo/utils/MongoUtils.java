package com.asb.analytics.mongo.utils;

import com.asb.analytics.bigquery.EventUpdate;
import com.asb.analytics.domain.User;
import com.asb.analytics.domain.mercurius.FairOdd;
import com.asb.analytics.exceptions.EventUpdatesNotFoundException;
import com.asb.analytics.exceptions.UserNotFoundException;
import com.asb.analytics.logs.EventLogModel;
import com.asb.analytics.logs.Logger;
import com.asb.analytics.util.CalendarUtils;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MongoUtils {

    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final String USERS = "users";
    private static final String EVENT_UPDATES = "events_updates";
    private static final String EVENT_FAIR_PRICES = "event_fair_prices";
    private static final String EVENTS_LOG = "events_log";
    private static final String DAILY_EVENTS_IDS = "daily_events_ids";
    private final MongoDatabase database;
    private MongoCollection<Document> mongoCollection;
    private final Gson gson = new Gson();

    private MongoUtils(MongoDatabase database) {
        this.database = database;
    }

    public static MongoUtils query(MongoDatabase database) {
        return new MongoUtils(database);
    }

    public MongoUtils collection(String collection) {
        mongoCollection = database.getCollection(collection);
        return this;
    }

    public List<Integer> eventIdsInDateRange(Date start, Date end) {

        BasicDBObject query = new BasicDBObject();
        query.put("openDate", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());

        List<Integer> eventIds = new ArrayList<>();

        try (MongoCursor<Document> cursor = mongoCollection.find(query).iterator()) {
            while (cursor.hasNext()) {
                eventIds.add(Integer.valueOf(cursor.next().getString("eventId")));
            }
        }

        return eventIds;
    }

    /**
     * Query mongodb to retrieve all the users.
     *
     * @return list of {@link User}
     */
    public List<User> getUsers() throws UserNotFoundException {

        MongoCollection collection = database.getCollection(USERS);

        List<User> users = new ArrayList<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                users.add(gson.fromJson(cursor.next().toJson(), User.class));
            }
        }

        if (users.size() == 0)
            throw new UserNotFoundException("There are no users in the collection " + USERS);

        return users;
    }

    /**
     * Query mongodb to retrieve all the event updates by event id.
     *
     * @return list of {@link EventUpdate}
     */
    public List<EventUpdate> getEventUpdates(Integer eventId) throws EventUpdatesNotFoundException {

        MongoCollection collection = database.getCollection(EVENT_UPDATES);

        List<EventUpdate> eventUpdates = new ArrayList<>();

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("eventId", eventId);

        try (MongoCursor<Document> cursor = collection.find(whereQuery).iterator()) {
            while (cursor.hasNext()) {
                eventUpdates.add(gson.fromJson(cursor.next().toJson(), EventUpdate.class));
            }
        }

        if (eventUpdates.size() == 0)
            throw new EventUpdatesNotFoundException("");

        return eventUpdates;
    }

    public boolean saveEventUpdate(List<Document> documents, String collectionName) {

        MongoCollection collection = database.getCollection(collectionName);

        try {
            collection.insertMany(documents);
        } catch (Exception e) {
            Logger.log().error("SAVE " + collectionName + " MONGO ERROR");
            Logger.log().error(e.getMessage());
            return false;
        }

        return true;
    }

    public List<FairOdd> getFairOdds() {

        MongoCollection collection = database.getCollection(EVENT_FAIR_PRICES);

        List<FairOdd> fairOdds = new ArrayList<>();

        Date start = CalendarUtils.softwareStartDateTime();
        Date end = CalendarUtils.softwareStopDateTime();

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("startDate", BasicDBObjectBuilder.start("$gte", dateTimeFormat.format(start)).add("$lte", dateTimeFormat.format(end)).get());

        try (MongoCursor<Document> cursor = collection.find(whereQuery).iterator()) {
            while (cursor.hasNext()) {
                fairOdds.add(gson.fromJson(cursor.next().toJson(), FairOdd.class));
            }
        }

        return fairOdds;
    }

    public List<EventLogModel> getEventsLog() {

        MongoCollection collection = database.getCollection(EVENTS_LOG);

        List<EventLogModel> eventLogs = new ArrayList<>();

        String today = CalendarUtils.todayDate();

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("date", today);

        try (MongoCursor<Document> cursor = collection.find(whereQuery).iterator()) {
            while (cursor.hasNext()) {
                eventLogs.add(gson.fromJson(cursor.next().toJson(), EventLogModel.class));
            }
        }

        return eventLogs;
    }

    public void addAllEventsIds(List<Document> documents) {
        MongoCollection collection = database.getCollection(DAILY_EVENTS_IDS);

        try {
            collection.insertMany(documents);
        } catch (Exception e) {
            Logger.log().error("SAVE " + DAILY_EVENTS_IDS + " MONGO ERROR");
            Logger.log().error(e.getMessage());
        }
    }

    public void cleanAllEventsIds() {

        MongoCollection collection = database.getCollection(DAILY_EVENTS_IDS);

        BasicDBObject document = new BasicDBObject();

        // Delete All documents from collection Using blank BasicDBObject
        collection.deleteMany(document);
    }

    public List<String> getAllEventsIds() {

        MongoCollection collection = database.getCollection(DAILY_EVENTS_IDS);

        List<String> eventIds = new ArrayList<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                eventIds.add(gson.fromJson(cursor.next().toJson(), String.class));
            }
        }

        return eventIds;
    }
}
