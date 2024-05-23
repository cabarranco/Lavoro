package com.asbresearch.pulse.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.util.CollectionUtils.isEmpty;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class StrategyCriteria {
    private final List<StrategyRule> rules;

    @JsonCreator(mode = Mode.PROPERTIES)
    public StrategyCriteria(@JsonProperty("rules") List<StrategyRule> rules) {
        checkArgument(!isEmpty(rules), "Strategy rules must not be empty");
        this.rules = ImmutableList.copyOf(rules);
    }

    public static StrategyCriteria of(StrategyRule... rules) {
        return new StrategyCriteria(Arrays.asList(rules));
    }
}
