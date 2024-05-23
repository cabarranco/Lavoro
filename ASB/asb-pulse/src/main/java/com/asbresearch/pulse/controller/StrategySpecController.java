package com.asbresearch.pulse.controller;

import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.ScheduleConfig;
import com.asbresearch.pulse.model.StrategySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.swagger.annotations.ApiParam;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.asbresearch.pulse.util.Constants.DATE_FORMATTER;
import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.util.CollectionUtils.isEmpty;


@RestController
@RequestMapping(value = "/strategySpec", produces = "application/json")
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
class StrategySpecController {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ScheduleConfig scheduleConfig;

    @GetMapping
    public List<StrategySpec> getStrategySpec() throws IOException {
        List<StrategySpec> result = getSpecFor(LocalDate.now());
        log.info("Getting default strategy spec.. {}", result);
        return result;
    }

    @GetMapping(path = "/{date}")
    public List<StrategySpec> getStrategySpec(@ApiParam(value = "date", example = "2020-01-27") @PathVariable("date") String date) throws IOException {
        List<StrategySpec> result = getSpecFor(LocalDate.parse(date, DATE_FORMATTER));
        log.info("Getting default strategy spec.. {}", result);
        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createStrategySpec(@RequestBody List<StrategySpec> strategySpecs) throws IOException {
        checkArgument(!isEmpty(strategySpecs), "strategySpecs must not be empty");
        checkArgument(isUniqueStratIds(strategySpecs), "strategyId must be unique");
        String date = LocalDate.now().format(DATE_FORMATTER);
        String json = mapper.writeValueAsString(strategySpecs);
        Path path = Paths.get(String.format("%s/strategy/spec/%s/strategy.json", appProperties.getDataDirectory(), date));
        log.info("Writing strategy spec to dir={}/strategy/spec", appProperties.getDataDirectory());
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
        Files.write(path, Collections.singleton(json), StandardCharsets.UTF_8);
        return "Created";
    }

    private boolean isUniqueStratIds(@RequestBody List<StrategySpec> strategySpecs) {
        return strategySpecs.stream().map(strategySpec -> strategySpec.getStrategyId()).collect(Collectors.toSet()).size() == strategySpecs.size();
    }

    @GetMapping(path = "/demo/{multiplier}", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StrategySpec> createDemoStrategySpec(@ApiParam(value = "multiplier", example = "10") @PathVariable("multiplier") int multiplier) throws IOException {
        return scheduleConfig.createDemoStrategy(multiplier);
    }

    protected List<StrategySpec> getSpecFor(LocalDate date) throws IOException {
        File strategyFile = new File(String.format("%s/strategy/spec/%s", appProperties.getDataDirectory(), date.format(DATE_FORMATTER)), "strategy.json");
        if (!strategyFile.exists()) {
            return Collections.emptyList();
        }
        String json = loadStrategiesJson(strategyFile);
        CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, StrategySpec.class);
        return mapper.readValue(json, javaType);
    }

    protected String loadStrategiesJson(File strategyFile) throws IOException {
        return new String(Files.readAllBytes(ResourceUtils.getFile(strategyFile.getAbsolutePath()).toPath()));
    }
}
