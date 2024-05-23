package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.mapping.BetfairMarketTypeMapping;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.BetfairEventService;
import com.asbresearch.pulse.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
@EnableConfigurationProperties({AppProperties.class, StrategyProperties.class})
@Slf4j
@ConditionalOnProperty(prefix = "strategy", name = "file", havingValue = "on")
public class FileStrategyProvider extends AbstractStrategyProvider implements StrategyProvider {
    private final ObjectMapper mapper;
    private final AppProperties appProperties;

    @Autowired
    public FileStrategyProvider(
            BetfairEventService betfairEventService,
            BetfairMarketTypeMapping marketTypeMapping,
            BetfairInPlayService betfairInPlayService,
            ObjectMapper mapper,
            AppProperties appProperties,
            StrategyProperties strategyProperties,
            StrategyEventIgnoreContainer strategyEventIgnoreContainer) {

        super(betfairEventService, marketTypeMapping, betfairInPlayService, strategyProperties, strategyEventIgnoreContainer, appProperties);

        this.mapper = mapper;
        this.appProperties = appProperties;
    }

    @Override
    public List<StrategySpec> getCurrentStrategySpec() {
        File strategyFile = new File(String.format("%s/strategy/spec/%s", appProperties.getDataDirectory(), LocalDate.now().format(Constants.DATE_FORMATTER)), "strategy.json");
        if (!strategyFile.exists()) {
            return Collections.emptyList();
        }
        try {
            String json = FileUtils.readFileToString(strategyFile, StandardCharsets.UTF_8);
            CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, StrategySpec.class);
            return mapper.readValue(json, javaType);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading current strategy spec", ex);
        }
    }
}
