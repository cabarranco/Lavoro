package com.asbresearch.common.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@ConfigurationProperties("bigquery")
public class BigQueryProperties {
    @NestedConfigurationProperty
    private final Credentials credentials = new Credentials();
    @NestedConfigurationProperty
    private final Credentials secondaryCredentials = new Credentials();
    private String projectId = "asbanalytics";
    private String location = "europe-west2";
    private String secondaryProjectId;
    private String dataDir = ".";
    private String fieldDelimiter = "|";
    private Map<String, Integer> createdDateTableMappings = Stream.of(new String[][]{
            {"betstore.betfair_events", "6"},
            {"betstore.betfair_events_mapping_exceptions", "6"},
            {"betstore.betfair_historical_data", "13"},
            {"betstore.betfair_market_catalogue", "10"},
            {"betstore.betfair_soccer_inplay", "7"},
            {"betstore.betfair_sofascore_event_mapping", "2"},
            {"betstore.sofascore_event_incidents", "7"},
            {"betstore.sofascore_events", "5"},
            {"betstore.sofascore_soccer_inplay", "7"},
            {"research.event_inplay_features", "20"},
            {"research.event_prelive_features", "11"},
            {"research.event_prices_analytics", "24"},
            {"research.inplay_event_details", "8"},
            {"research.inplay_event_exceptions", "3"},
    }).collect(Collectors.toMap(data -> data[0], data -> Integer.valueOf(data[1])));

    public File getDataDir() {
        String path = StringUtils.trimToNull(dataDir);
        if (path == null) {
            path = ".";
        }
        return new File(path);
    }
}
