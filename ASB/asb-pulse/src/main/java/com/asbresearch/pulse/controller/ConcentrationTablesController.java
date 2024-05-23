package com.asbresearch.pulse.controller;

import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.model.ConcentrationTablesResult;
import com.asbresearch.pulse.service.plm.ConcentrationTables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/concentrationTables", produces = "application/json")
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
class ConcentrationTablesController {
    @Autowired
    private ConcentrationTables concentrationTables;

    @GetMapping
    public ConcentrationTablesResult getConcentrationTables() {
        return ConcentrationTablesResult.of(concentrationTables);
    }
}
