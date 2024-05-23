package com.asbresearch.betfair.ref.entities;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class EventTest {

    @Test
    public void toJson() throws JsonProcessingException {
        Instant instant = Instant.ofEpochMilli(1580809522950L);
        Event event = Event.of("29676731", "Maccabi Tel Aviv v Hapoel Raanana", "IL", "GMT", instant);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(event);
        Assert.assertThat(json, notNullValue());
        Assert.assertThat(json, is("{\"id\":\"29676731\",\"name\":\"Maccabi Tel Aviv v Hapoel Raanana\",\"countryCode\":\"IL\",\"timezone\":\"GMT\",\"openDate\":\"2020-02-04T09:45:22.950Z\"}"));
    }
}