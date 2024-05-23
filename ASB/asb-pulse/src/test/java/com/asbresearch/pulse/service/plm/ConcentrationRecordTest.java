package com.asbresearch.pulse.service.plm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
public class ConcentrationRecordTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void cycle() throws Exception {
        ConcentrationRecord concentrationRecord = new ConcentrationRecord("1.734567", 1000, 34.0);
        String json = mapper.writeValueAsString(concentrationRecord);
        assertThat(json, notNullValue());
        assertThat(json, is("{\"id\":\"1.734567\",\"maxTradingDayAvailableBalance\":1000.0,\"usedBalance\":34.0}"));

        ConcentrationRecord fromJson = mapper.readValue(json, ConcentrationRecord.class);
        assertThat(fromJson, notNullValue());
        assertThat(fromJson.getId(), is("1.734567"));
        assertThat(fromJson.getMaxTradingDayAvailableBalance(), is(1000.0));
        assertThat(fromJson.getUsedBalance(), is(34.0));
    }
}