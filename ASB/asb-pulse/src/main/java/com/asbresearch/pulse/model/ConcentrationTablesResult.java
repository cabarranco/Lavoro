package com.asbresearch.pulse.model;

import com.asbresearch.pulse.service.plm.ConcentrationRecord;
import com.asbresearch.pulse.service.plm.ConcentrationTables;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConcentrationTablesResult {
    private Map<String, ConcentrationRecord> event = null;
    private Map<String, ConcentrationRecord> strategy = null;
    private Map<String, ConcentrationRecord> strategyEvent = null;

    @JsonCreator(mode = PROPERTIES)
    public ConcentrationTablesResult(@JsonProperty("event") Map<String, ConcentrationRecord> event,
                                     @JsonProperty("strategy") Map<String, ConcentrationRecord> strategy,
                                     @JsonProperty("strategyEvent") Map<String, ConcentrationRecord> strategyEvent) {
        if (!CollectionUtils.isEmpty(event)) {
            this.event = event;
        }
        if (!CollectionUtils.isEmpty(strategy)) {
            this.strategy = strategy;
        }
        if (!CollectionUtils.isEmpty(strategyEvent)) {
            this.strategyEvent = strategyEvent;
        }
    }

    public static ConcentrationTablesResult of(ConcentrationTables concentrationTables) {
        return new ConcentrationTablesResult(concentrationTables.getEvents().getRecords(),
                concentrationTables.getStrategies().getRecords(),
                concentrationTables.getStrategyEvents().getRecords());
    }
}
