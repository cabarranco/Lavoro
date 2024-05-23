package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;


@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class Event {
	@EqualsAndHashCode.Include
	private final String id;
	private final String name;
	private final String countryCode;
	private final String timezone;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant openDate;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Event(@JsonProperty("id") String id,
                 @JsonProperty("name") String name,
                 @JsonProperty("countryCode") String countryCode,
                 @JsonProperty("timezone") String timezone,
                 @JsonProperty("openDate") Instant openDate) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
        this.timezone = timezone;
        this.openDate = openDate;
    }

	public static Event of(String id,
                           String name,
                           String countryCode,
                           String timezone,
                           Instant openDate) {
        return new Event(id, name, countryCode, timezone, openDate);
	}

	public static Event of(String id,
                           String name,
                           String countryCode,
                           String timezone) {
        return of(id, name, countryCode, timezone, Instant.now());
	}
}
