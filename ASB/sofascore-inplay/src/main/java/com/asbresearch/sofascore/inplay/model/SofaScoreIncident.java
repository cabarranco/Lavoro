package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

import static com.asbresearch.common.BigQueryUtil.csvValue;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreIncident {
    @JsonIgnore
    String id;
    @JsonIgnore
    String eventId;
    String text;
    @JsonIgnore
    int index;
    int time;
    String incidentClass;
    String incidentType;
    @JsonIgnore
    String json;
    @JsonIgnore
    Instant createTimestamp;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreIncident(@JsonProperty("text") String text,
                             @JsonProperty("time") int time,
                             @JsonProperty("incidentClass") String incidentClass,
                             @JsonProperty("incidentType") String incidentType) {
        this.text = text;
        this.time = time;
        this.incidentClass = incidentClass;
        this.incidentType = incidentType;
    }

    public String toCsv() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                csvValue(id),
                csvValue(eventId),
                csvValue(index),
                csvValue(time),
                csvValue(incidentClass),
                csvValue(incidentType),
                csvValue(json),
                csvValue(createTimestamp));
    }
}
