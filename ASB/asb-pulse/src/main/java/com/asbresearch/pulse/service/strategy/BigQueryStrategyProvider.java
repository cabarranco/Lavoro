package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.BetfairEventService;
import com.asbresearch.pulse.util.RetryUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@EnableConfigurationProperties({AppProperties.class, StrategyProperties.class})
@Slf4j
@ConditionalOnProperty(prefix = "strategy", name = "bg", havingValue = "on")
public class BigQueryStrategyProvider extends AbstractStrategyProvider implements StrategyProvider {
    private final ObjectMapper mapper;
    private final AppProperties appProperties;
    private final BigQueryService bigQueryService;

    @Autowired
    public BigQueryStrategyProvider(
            BigQueryService bigQueryService,
            BetfairEventService betfairEventService,
            BetfairMarketTypeMapping marketTypeMapping,
            BetfairInPlayService betfairInPlayService,
            ObjectMapper mapper,
            AppProperties appProperties,
            StrategyProperties strategyProperties,
            StrategyEventIgnoreContainer strategyEventIgnoreContainer) {

        super(betfairEventService, marketTypeMapping, betfairInPlayService, strategyProperties, strategyEventIgnoreContainer, appProperties);

        this.bigQueryService = bigQueryService;
        this.mapper = mapper;
        this.appProperties = appProperties;
    }

    @Override
    public List<StrategySpec> getCurrentStrategySpec() {
        CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, StrategySpec.class);
        while (true) {
            String sql = String.format("SELECT json from `pulse_reporting.strategy_meta` where strategyId in ( SELECT strategyId FROM `pulse_reporting.strategies` where betDate <= current_date() and node = '%s' and isActive is true order by betDate desc limit 1 )", appProperties.getNode());
            log.info("Running sql={}", sql);
            try {
                List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(sql);
                if (!isEmpty(resultSet)) {
                    Optional<Object> json = resultSet.iterator().next().get("json");
                    if (json.isPresent()) {
                        try {
                            return mapper.readValue((String) json.get(), javaType);
                        } catch (JsonProcessingException e) {
                            log.error("Interrupted with executing bigQuery sql={}", sql, e);
                        }
                    }
                }
                return Collections.emptyList();
            } catch (InterruptedException e) {
                RetryUtil.retryWait("Error executing sql=" + sql);
            }
        }
    }
}
