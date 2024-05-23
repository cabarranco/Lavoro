package com.asbresearch.sofascore.inplay;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.sofascore.inplay.model.SofaScoreIncident;
import com.asbresearch.sofascore.inplay.model.SofaScoreIncidents;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.asbresearch.sofascore.inplay.util.SofaScoreConstant.BASE_URL;

@Slf4j
class SofaScoreIncidentClientTest {
    private static final String EVENT_ID = "9426107";
    ObjectMapper objectMapper = new ObjectMapper();
    private SofaScoreIncidentClient sofaScoreIncidentClient;

    @BeforeEach
    void setUp() {
        sofaScoreIncidentClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(SofaScoreIncidentClient.class))
                .logLevel(Logger.Level.FULL)
                .target(SofaScoreIncidentClient.class, BASE_URL);
    }

    @Test
    void getIncidents() throws IOException {
        Instant now = Instant.now();
        Response response = sofaScoreIncidentClient.getIncidents(EVENT_ID);
        if (response.status() == 200) {
            String json = IOUtils.toString(response.body().asInputStream(), Charset.defaultCharset());
            response.body().close();
            log.info("json={}", json);
            SofaScoreIncidents incidents = objectMapper.readValue(json, SofaScoreIncidents.class);
            Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            List<Object> incidentsMap = (List<Object>) jsonMap.getOrDefault("incidents", new ArrayList<>());
            int counter = 0;
            for (SofaScoreIncident sofaScoreIncident : incidents.getIncidents()) {
                sofaScoreIncident.setEventId(EVENT_ID);
                sofaScoreIncident.setId(BigQueryUtil.shortUUID());
                Map<String, Object> incidentMap = (Map<String, Object>) incidentsMap.get(counter++);
                sofaScoreIncident.setJson(objectMapper.writeValueAsString(incidentMap));
                sofaScoreIncident.setCreateTimestamp(now);
            }
            incidents.getIncidents().forEach(sofaScoreIncident -> log.info("{}", sofaScoreIncident));
        }
    }
}