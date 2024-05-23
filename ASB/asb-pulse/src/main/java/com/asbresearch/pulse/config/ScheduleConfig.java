package com.asbresearch.pulse.config;

import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.asbresearch.pulse.model.StrategySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.asbresearch.pulse.util.Constants.DATE_FORMATTER;
import static java.util.stream.Collectors.toList;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(StrategyProperties.class)
@Slf4j
public class ScheduleConfig {
    private final ObjectMapper mapper;
    private final AppProperties appProperties;

    @Autowired
    public ScheduleConfig(ObjectMapper mapper, AppProperties appProperties) {
        this.mapper = mapper;
        this.appProperties = appProperties;
    }

    public List<StrategySpec> createDemoStrategy(int multiplier) throws IOException {
        String json = Resources.toString(Resources.getResource("strategy.json"), Charsets.UTF_8);
        List<StrategySpec> specs = copies(json, multiplier);
        writeStrategy(specs);
        return specs;
    }

    private List<StrategySpec> copies(String json, int multiplier) throws JsonProcessingException {
        StrategySpec[] strategySpecs = mapper.readValue(json, StrategySpec[].class);
        if (multiplier < 2) {
            return Arrays.asList(strategySpecs);
        }
        List<StrategySpec> result = new ArrayList<>();
        for (StrategySpec strategySpec : strategySpecs) {
            for (int i = 0; i < multiplier; i++) {
                result.add(new StrategySpec(String.format("%s%s", strategySpec.getStrategyId(), i), strategySpec.getAllocatorId(),
                        strategySpec.getHedgeStrategyId(),
                        strategySpec.getEventCriteria(),
                        strategySpec.getBookRunnersCompute().stream().map(UserRunnerCode::getCode).collect(toList()),
                        strategySpec.getBookRunnersAllocator().stream().map(UserRunnerCode::getCode).collect(toList()),
                        strategySpec.getStrategyCriteria()));
            }
        }
        return result;
    }

    private void writeStrategy(List<StrategySpec> strategySpecs) {
        try {
            String date = LocalDate.now().format(DATE_FORMATTER);
            String json = mapper.writeValueAsString(strategySpecs);
            Path path = Paths.get(String.format("%s/strategy/spec/%s/strategy.json", appProperties.getDataDirectory(), date));
            log.info("Strategy spec size={} written to directory={}/strategy/spec", strategySpecs.size(), appProperties.getDataDirectory());
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            Files.write(path, Collections.singleton(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error while creating daily strategy spec", e);
        }
    }
}
