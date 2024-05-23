package com.asbresearch.pulse.controller;

import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.config.StrategyProperties;
import com.asbresearch.pulse.model.OpportunityBet;
import com.asbresearch.pulse.model.OpportunityBetsRequest;
import com.asbresearch.pulse.model.OpportunityBetsResult;
import com.asbresearch.pulse.model.StrategySpec;
import com.asbresearch.pulse.service.strategy.StrategyEngine;
import com.asbresearch.pulse.service.strategy.StrategyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiParam;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.asbresearch.pulse.util.Constants.DATE_FORMATTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_COMPARATOR;

@Slf4j
@EnableConfigurationProperties({StrategyProperties.class, AppProperties.class})
@RestController
@RequestMapping(value = "/strategyEngine", produces = "application/json")
public class StrategyEngineController {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private StrategyEngine strategyEngine;
    @Autowired
    private StrategyProvider strategyProvider;

    @PostMapping(path = "/opportunities")
    public OpportunityBetsResult opportunityBets(@RequestBody OpportunityBetsRequest opportunityBetsRequest) throws IOException {
        LocalDate requestDate = LocalDate.parse(opportunityBetsRequest.getDate(), DATE_FORMATTER);
        int totalRows = 0;
        List<OpportunityBet> trades = new ArrayList<>();
        for (String strategyId : opportunityBetsRequest.getStrategyIds()) {
            File dir = new File(String.format("%s/strategy/opportunities/%s/%s", appProperties.getDataDirectory(), requestDate.format(DATE_FORMATTER), strategyId));
            if (dir.exists()) {
                File[] files = dir.listFiles();
                totalRows += files.length;
                Arrays.sort(files, LASTMODIFIED_COMPARATOR.reversed());
                for (File f : files) {
                    String json = FileUtils.readFileToString(f, UTF_8);
                    OpportunityBet trade = mapper.readValue(json, OpportunityBet.class);
                    trades.add(trade);
                }
            }
        }
        if (!trades.isEmpty()) {
            int slice = opportunityBetsRequest.getIndex() - 1;
            List<List<OpportunityBet>> partition = Lists.partition(trades, appProperties.getMaxOpportunityBets());
            if (opportunityBetsRequest.getIndex() < 1 || opportunityBetsRequest.getIndex() > partition.size()) {
                slice = 0;
            }
            return new OpportunityBetsResult(totalRows, partition.get(slice));
        }
        return new OpportunityBetsResult(totalRows, trades);
    }

    @GetMapping(path = "/strategyId/{date}")
    public List<String> getStrategyIds(@ApiParam(value = "date", example = "2020-01-27") @PathVariable("date") String date) {
        LocalDate requestDate = LocalDate.parse(date, DATE_FORMATTER);
        File dir = new File(String.format("%s/strategy/opportunities/%s", appProperties.getDataDirectory(), requestDate.format(DATE_FORMATTER)));
        List<String> result = new ArrayList<>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    result.add(file.getName());
                }
            }
        }
        return result;
    }

    @GetMapping(path = "/{trackId}")
    public List<String> getLogEntries(@ApiParam(value = "trackId", example = "12345678") @PathVariable("trackId") String trackId) throws IOException {
        List<String> result = new ArrayList<>();
        Path path = Paths.get(appProperties.getLogDirectory(), "pulse.log");
        Stream<String> lines = Files.lines(path);
        result.addAll(lines.filter(line -> line.contains(trackId)).collect(Collectors.toList()));
        lines.close();
        File archivedDir = new File(String.format("%s/archived", appProperties.getLogDirectory()));
        if (archivedDir.exists()) {
            File[] files = archivedDir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    lines = Files.lines(Paths.get(file.toURI()));
                    result.addAll(lines.filter(line -> line.contains(trackId)).collect(Collectors.toList()));
                    lines.close();
                }
            }
        }
        return result;
    }


    @GetMapping(path = "/strategySpec")
    public List<StrategySpec> getCurrentStrategySpecs() {
        return strategyProvider.getCurrentStrategySpec();
    }
}
