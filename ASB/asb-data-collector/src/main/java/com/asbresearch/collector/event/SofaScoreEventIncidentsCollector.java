package com.asbresearch.collector.event;

import com.asbresearch.collector.config.CollectorProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.SofaScoreEventIncidentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.asbresearch.collector.util.Constants.startDate;

@Service
@Slf4j
@EnableConfigurationProperties({CollectorProperties.class})
@ConditionalOnProperty(prefix = "collector", name = "sofaScoreEventIncidentsCollector", havingValue = "on")
public class SofaScoreEventIncidentsCollector {
    private SofaScoreEventIncidentsService sofaScoreEventIncidentsService;
    private final BigQueryService bigQueryService;
    private final CollectorProperties collectorProperties;

    @Autowired
    public SofaScoreEventIncidentsCollector(BigQueryService bigQueryService, CollectorProperties collectorProperties) {
        this.bigQueryService = bigQueryService;
        this.collectorProperties = collectorProperties;
    }

    @PostConstruct
    public void start() {
        sofaScoreEventIncidentsService = new SofaScoreEventIncidentsService(bigQueryService,
                collectorProperties.getEventIncidentsFrequencyInSec() * 1000,
                startDate(collectorProperties));
    }

    @PreDestroy
    public void stopEventMapping() {
        if (sofaScoreEventIncidentsService != null) {
            sofaScoreEventIncidentsService.stop();
        }
    }
}
