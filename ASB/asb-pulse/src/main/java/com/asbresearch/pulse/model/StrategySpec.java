package com.asbresearch.pulse.model;

import com.asbresearch.pulse.mapping.UserRunnerCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

import static com.asbresearch.pulse.model.StrategyRule.FUNCTIONS;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StrategySpec {
    private final String strategyId;
    private final String allocatorId;
    private final String hedgeStrategyId;

    private final EventCriteria eventCriteria;
    private final List<UserRunnerCode> bookRunnersCompute;
    private final List<UserRunnerCode> bookRunnersAllocator;
    private final StrategyCriteria strategyCriteria;

    @JsonCreator(mode = PROPERTIES)
    public StrategySpec(@JsonProperty("strategyId") String strategyId,
                        @JsonProperty("allocatorId") String allocatorId,
                        @JsonProperty("hedgeStrategyId") String hedgeStrategyId,
                        @JsonProperty("eventCriteria") EventCriteria eventCriteria,
                        @JsonProperty("bookRunnersCompute") List<String> bookRunnersCompute,
                        @JsonProperty("bookRunnersAllocator") List<String> bookRunnersAllocator,
                        @JsonProperty("strategyCriteria") StrategyCriteria strategyCriteria) {
        checkNotNull(strategyId, "StrategyId must be provided");
        checkNotNull(allocatorId, "AllocatorId must be provided");
        checkNotNull(hedgeStrategyId, "HedgeStrategyId must be provided");
        checkNotNull(eventCriteria, "EventCriteria must be provided");
        checkArgument(!isEmpty(bookRunnersCompute), "BookRunnersCompute must not be empty");
        checkNotNull(strategyCriteria, "StrategyCriteria must be provided");
        checkArgument(ruleVarsInBookRunners(bookRunnersCompute, strategyCriteria), "Rule names must be present in BookRunnersCompute codes");
        checkArgument(!isEmpty(bookRunnersAllocator), "BookRunnersAllocator must not be empty");
        checkArgument(allocatorsInCompute(bookRunnersAllocator, bookRunnersCompute), "BookRunnersAllocator must be present in BookRunnersCompute");

        Map<String, UserRunnerCode> userRunnerCodeMap = createBookRunnersMapping(bookRunnersCompute);

        this.strategyId = strategyId;
        this.allocatorId = allocatorId;
        this.hedgeStrategyId = hedgeStrategyId;
        this.eventCriteria = eventCriteria;
        this.bookRunnersCompute = bookRunnersCompute.stream().map(s -> userRunnerCodeMap.get(s)).collect(Collectors.toUnmodifiableList());
        this.bookRunnersAllocator = bookRunnersAllocator.stream().map(s -> userRunnerCodeMap.get(s)).collect(Collectors.toUnmodifiableList());
        this.strategyCriteria = strategyCriteria;
    }

    private boolean allocatorsInCompute(List<String> bookRunnersAllocator, List<String> bookRunnersCompute) {
        return bookRunnersCompute.containsAll(bookRunnersAllocator);
    }

    private Map<String, UserRunnerCode> createBookRunnersMapping(List<String> bookRunnersCompute) {
        Map<String, UserRunnerCode> mapping = new HashMap<>();
        bookRunnersCompute.forEach(s -> mapping.put(s, new UserRunnerCode(s)));
        return mapping;
    }

    private boolean ruleVarsInBookRunners(List<String> bookRunners, StrategyCriteria strategyCriteria) {
        Set<String> allVars = strategyCriteria.getRules().stream().map(StrategyRule::getVars).flatMap(vars -> vars.stream()).collect(Collectors.toSet());
        List<String> allRules = Lists.newArrayList(bookRunners);
        allRules.addAll(FUNCTIONS);
        return allRules.containsAll(allVars);
    }
}
