package com.asbresearch.sofascore.inplay.task;

import com.asbresearch.common.BigQueryUtil;
import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.sofascore.inplay.SofaScoreIncidentClient;
import com.asbresearch.sofascore.inplay.model.SofaScoreIncident;
import com.asbresearch.sofascore.inplay.model.SofaScoreIncidents;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class SofaScoreEventIncidentsTask extends TimerTask {
    private final SofaScoreIncidentClient incidentClient;
    private final BigQueryService bigQueryService;
    private final Map<String, Boolean> completed = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String startDate;

    public SofaScoreEventIncidentsTask(BigQueryService bigQueryService,
                                       SofaScoreIncidentClient incidentClient,
                                       String startDate) {
        this.incidentClient = incidentClient;
        this.bigQueryService = bigQueryService;
        this.startDate = startDate;
    }

    @Override
    public void run() {
        try {
            Set<String> pendingEvents = pendingEvents().stream()
                    .filter(eventId -> !completed.containsKey(eventId))
                    .collect(Collectors.toSet());
            log.info("Currently {} events", pendingEvents.size());
            pendingEvents.forEach(eventId -> {
                Response response = null;
                try {
                    response = incidentClient.getIncidents(String.valueOf(eventId));
                    if (response.status() == 200) {
                        String json = IOUtils.toString(response.body().asInputStream(), Charset.defaultCharset());
                        log.debug("eventId={} json={}", eventId, json);
                        persistScores(eventId, json);
                    }
                } catch (IOException e) {
                    log.error("Error reading json incidents from SofaScore for eventId={}", eventId, e);
                } finally {
                    if (response != null) {
                        try {
                            response.body().close();
                        } catch (IOException e) {
                            log.error("Error closing response body for eventId={}", eventId, e);
                        }
                    }
                }
            });
        } catch (RuntimeException ex) {
            log.error("Error reading event incidents from SofaScore", ex);
        }
    }

    private void persistScores(String eventId, String json) {
        Instant now = Instant.now();
        try {
            SofaScoreIncidents incidents = objectMapper.readValue(json, SofaScoreIncidents.class);
            Optional<SofaScoreIncident> first = incidents.getIncidents().stream().findFirst();
            if (first.isPresent() && first.get().getText() != null && first.get().getText().equals("FT")) {
                Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<>() {
                });
                List<Object> incidentsMap = (List<Object>) jsonMap.getOrDefault("incidents", new ArrayList<>());
                int counter = 0;
                for (SofaScoreIncident sofaScoreIncident : incidents.getIncidents()) {
                    sofaScoreIncident.setEventId(String.valueOf(eventId));
                    sofaScoreIncident.setId(BigQueryUtil.shortUUID());
                    sofaScoreIncident.setIndex(counter + 1);
                    Map<String, Object> incidentMap = (Map<String, Object>) incidentsMap.get(counter);
                    sofaScoreIncident.setJson(objectMapper.writeValueAsString(incidentMap));
                    sofaScoreIncident.setCreateTimestamp(now);
                    counter++;
                }
                List<String> incidentRows = incidents.getIncidents()
                        .stream()
                        .map(SofaScoreIncident::toCsv)
                        .collect(Collectors.toList());
                log.info("loading {} incidents for eventId={}", incidentRows.size(), eventId);
                bigQueryService.insertRows(BigQueryUtil.BETSTORE_DATASET, "sofascore_event_incidents", incidentRows);
                completed.put(eventId, Boolean.TRUE);
            }
        } catch (IOException | RuntimeException ex) {
            log.error("Error converting incidents json for eventId={}", eventId, ex);
        }
    }

    private Set<String> pendingEvents() {
        List<Map<String, Optional<Object>>> resultSet = Collections.emptyList();
        try {
            String query = String.format("SELECT distinct id FROM `betstore.sofascore_events` where date(startTime) between '%s' and current_date()  " +
                    "except distinct " +
                    "SELECT distinct eventId FROM `betstore.sofascore_event_incidents`", startDate);
            log.info("sql={}", query);
            resultSet = bigQueryService.performQuery(query);
        } catch (InterruptedException e) {
            log.error("Error loading pending event ids for incidents");
        }
        return resultSet.stream().map(row -> row.get("id").get().toString()).collect(Collectors.toSet());
    }
}
