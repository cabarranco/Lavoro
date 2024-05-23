package com.asbresearch.pulse.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StrategySpecTest {

    @Test
    void loadStrategyJson() throws IOException {
        StrategySpec strategySpec = readStrategyFromResource();
        assertThat(strategySpec, notNullValue());
        System.out.println(strategySpec);
    }

    @Test
    void validDateRulesInBookRunnersCompute() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            List<String> bookRunnersCompute = Collections.singletonList("MO.H.B");
            EventCriteria eventCriteria = EventCriteria.liveAndPreLifeFootball(Sets.newHashSet(), ImmutableSet.of("0-1"), "0-0", 80);
            StrategyCriteria strategyCriteria = StrategyCriteria.of(StrategyRule.of("StratOddCriteria", Arrays.asList("CO.00.B"), "odd", "CO.00.B > 5.0"));
            new StrategySpec("strategyId",
                    "allocatorId",
                    "hedgeId",
                    eventCriteria,
                    bookRunnersCompute,
                    bookRunnersCompute, strategyCriteria);
        });
        assertTrue(exception.getMessage().contains("Rule names must be present in BookRunnersCompute codes"));
    }

    public static StrategySpec readStrategyFromResource() throws IOException {
        InputStream inputStream = StrategySpecTest.class.getClassLoader().getResourceAsStream("model/strategy.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(ByteStreams.toByteArray(inputStream), StrategySpec.class);
    }
}