package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.asbresearch.collector.derived.AnalyticEventsProvider.eventSql;
import static com.asbresearch.collector.util.CollectionUtils.isEmpty;

@Component("RemoveSecondHalfEndLessThan90Minutes")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "removeSecondHalfEndLessThan90Minutes", havingValue = "on")
@EnableConfigurationProperties({CollectorProperties.class})
@DependsOn({"InPlayUpdateTimeEpochFixer"})
public class RemoveSecondHalfEndLessThan90Minutes {
    private static final String COUNT_SQL = "select count(*) as total FROM `betstore.betfair_soccer_inplay` where updateType = 'SecondHalfEnd' and matchTime < 90 and eventId in (%s)";
    private final BigQueryService bigQueryService;
    private final String eventSql;

    @Autowired
    public RemoveSecondHalfEndLessThan90Minutes(BigQueryService bigQueryService, CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        eventSql = eventSql(collectorProperties);
    }

    @PostConstruct
    public void execute() {
        try {
            log.info("Begin remove synthetic second half end with matchTime < 90");
            int totalRemoved = 0;
            String sql = String.format(COUNT_SQL, eventSql);
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> result = bigQueryService.performQuery(sql);
            if (!isEmpty(result)) {
                totalRemoved = Integer.valueOf(result.iterator().next().get("total").get().toString());
            }
            sql = String.format(COUNT_SQL.replace("select count(*) as total", "delete"), eventSql);
            log.info("sql={}", sql);
            bigQueryService.performQuery(sql);
            log.info("Successfully removed {} synthetic second half end with matchTime < 90", totalRemoved);
        } catch (InterruptedException e) {
            log.error("Error occurred trying to remove synthetic second half end with matchTime < 90");
        }
    }
}
