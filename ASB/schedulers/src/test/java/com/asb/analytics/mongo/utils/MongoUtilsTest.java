package com.asb.analytics.mongo.utils;

import com.asb.analytics.bigquery.EventUpdate;
import com.asb.analytics.mongo.MongoConnector;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for mongo utils. Check all the read/write methods.
 *
 * Created by Claudio Paolicelli
 */
public class MongoUtilsTest {

    private MongoDatabase database;

    @Before
    public void setUp() throws Exception {
        this.database = new MongoConnector().connect();
    }

    @Test
    public void eventIdsInDateRange() {
    }

    @Test
    public void getUsers() {
    }

    /**
     * Test if the write method works as expected with normal data
     */
    @Test
    public void saveEventUpdate() {

        List<Document> eventUpdates = new ArrayList<>();

        eventUpdates.add(new EventUpdate(
                12345,
                "home",
                "Team Name",
                1,
                0,
                1,
                "2020-05-29 11:18:51.000984 UTC",
                0,
                "YellowCard",
                "FirstHalf",
                1,
                "0-0",
                "2020-05-29 11:18:51.000984 UTC"
        ).toDocument());

        boolean response = MongoUtils.query(database)
                .saveEventUpdate(eventUpdates, "events_updates");

        Assert.assertTrue(response);
    }
}