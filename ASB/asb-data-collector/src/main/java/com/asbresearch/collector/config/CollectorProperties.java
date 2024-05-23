package com.asbresearch.collector.config;

import feign.Logger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@ConfigurationProperties("collector")
public class CollectorProperties {
    private String marketTypeCodes = "MATCH_ODDS, OVER_UNDER_25, CORRECT_SCORE, OVER_UNDER_05, OVER_UNDER_15, OVER_UNDER_35, ASIAN_HANDICAP";
    private String cronExpression = "*/30 * * * * *";
    private Logger.Level feignLoggerLevel = Logger.Level.BASIC;
    private boolean betfairInPlaySave = true;
    private int betfairInPlayPollingFrequencyInSec = 120;
    private String startDate;
    private String endDate;
    private Map<String, String> partitionTables = Map.of("betfair_historical_data", "publishTime",
            "betfair_market_catalogue", "startTime",
            "event_prices_analytics", "timestamp",
            "event_inplay_features", "timestamp",
            "event_prelive_features", "timestamp");
    private List<String> ignoreDataReconcile = List.of("research.betfair_soccer_inplay_1970_staging");
    private boolean reconcileDeltaData = true;
    private int liveEventFrequencyInSec = (int) TimeUnit.MINUTES.toSeconds(1);
    private int eventIncidentsFrequencyInSec = (int) TimeUnit.MINUTES.toSeconds(1);
    private double jaroWinklerThreshold = 0.8;
    private int startTimeThresholdInSec = (int) TimeUnit.HOURS.toSeconds(1);
    private int sofaScoreSoccerInPlayThreads = 1;
    private int schedulerPoolSize = 20;
    private int historicalDataPageSize = 10000;
}
