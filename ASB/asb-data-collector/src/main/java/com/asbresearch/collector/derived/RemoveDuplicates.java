package com.asbresearch.collector.derived;

import com.asbresearch.common.bigquery.BigQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(prefix = "collector", name = "removeDuplicates", havingValue = "on")
public class RemoveDuplicates {

    private final BigQueryService bigQueryService;

    @PostConstruct
    public void removeDuplicates() {
        try {
            String inplaySoccerBackupSql = "CREATE OR REPLACE TABLE `research.betfair_soccer_inplay_20201104` AS SELECT * FROM `betstore.betfair_soccer_inplay`";
            bigQueryService.performQuery(inplaySoccerBackupSql);
            String inplaySoccerStagingSql = "CREATE OR REPLACE TABLE `research.betfair_soccer_inplay_staging` AS SELECT distinct * FROM `betstore.betfair_soccer_inplay`";
            bigQueryService.performQuery(inplaySoccerStagingSql);
            String inplaySoccerCopySql = "CREATE OR REPLACE TABLE `betstore.betfair_soccer_inplay` AS SELECT  * FROM `research.betfair_soccer_inplay_staging`";
            bigQueryService.performQuery(inplaySoccerCopySql);


            String ticksBackupSql = "CREATE OR REPLACE TABLE `research.betfair_historical_data_20201104` AS SELECT * FROM `betstore.betfair_historical_data`";
            bigQueryService.performQuery(ticksBackupSql);
            String ticksStagingSql = "CREATE OR REPLACE TABLE `research.betfair_historical_data_staging` AS SELECT distinct * FROM `betstore.betfair_historical_data`";
            bigQueryService.performQuery(ticksStagingSql);
            String ticksCopySql = "CREATE OR REPLACE TABLE `betstore.betfair_historical_data` AS SELECT  * FROM `research.betfair_historical_data_staging`";
            bigQueryService.performQuery(ticksCopySql);

        } catch (RuntimeException | InterruptedException ex) {
            log.error("Error when trying to remove duplicates", ex);
        }
    }
}
