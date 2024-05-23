package com.asbresearch.collector.derived;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.ThreadUtils;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component("SofaScoreSoccerInPlayCollector")
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "sofaScoreSoccerInPlayCollector", havingValue = "on")
public class SofaScoreSoccerInPlayCollector {
    private final BigQueryService bigQueryService;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final Map<String, Boolean> completed = new ConcurrentHashMap<>();

    @Autowired
    public SofaScoreSoccerInPlayCollector(BigQueryService bigQueryService, CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        this.threadPoolExecutor = createThreadPoolExecutor(collectorProperties.getSofaScoreSoccerInPlayThreads());
    }

    @Scheduled(cron = "${collector.sofaScoreSoccerInPlayCollector.cronExpression:*/60 * * * * *}")
    public void createSoccerInPlayRecords() {
        log.debug("Begin scheduled createSoccerInPlayRecords");
        try {
            Set<String> pendingEvents = getPendingEvents();
            log.info("{} events to process", pendingEvents.size());
            pendingEvents.forEach(eventId -> threadPoolExecutor.execute(new SofaScoreSoccerInPlayTask(bigQueryService, completed, eventId)));
        } catch (RuntimeException e) {
            log.error("Error getting event-ids to process from sofascore_event_incidents", e);
        } finally {
            log.debug("End scheduled createSoccerInPlayRecords");
        }
    }

    private ThreadPoolExecutor createThreadPoolExecutor(int nThreads) {
        ThreadFactory threadFactory = ThreadUtils.threadFactoryBuilder("SofaScoreSoccerInPlay").build();
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory);
    }

    private Set<String> getPendingEvents() {
        try {
            String sql = "SELECT distinct eventId  FROM `betstore.sofascore_event_incidents` where eventId not in ( SELECT  distinct eventId FROM `betstore.sofascore_soccer_inplay`)";
            log.info("sql={}", sql);
            List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
            return resultSet.stream()
                    .map(row -> row.get("eventId").get().toString())
                    .filter(eventId -> !completed.containsKey(eventId))
                    .collect(Collectors.toSet());
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error getting event-ids to process from sofascore_event_incidents", e);
        }
        return Collections.emptySet();
    }

    @PreDestroy
    public void shutDown() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
        }
    }
}
