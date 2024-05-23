package com.asb.analytics.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class MongoConnector {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "XUFpy9Zc8A6pRNtfhGgQHzSG";
    private static final String DB_NAME = "events_scheduler";
    private static final String HOST = "localhost";

    // COLLECTIONS
    public static final String COLLECTION_EVENTS = "events";

    public MongoDatabase connect(String dbname) {

        List<ServerAddress> seeds = new ArrayList<>();
        seeds.add( new ServerAddress( HOST, 27017 ));

        List<MongoCredential> credentials = new ArrayList<>();
        credentials.add(
                MongoCredential.createCredential(
                        USERNAME,
                        dbname,
                        PASSWORD.toCharArray()
                )
        );

        MongoClient mongoClient = new MongoClient(seeds, credentials);

        return mongoClient.getDatabase(dbname);
    }

    public MongoDatabase connect() {

        return connect(DB_NAME);
    }
}
