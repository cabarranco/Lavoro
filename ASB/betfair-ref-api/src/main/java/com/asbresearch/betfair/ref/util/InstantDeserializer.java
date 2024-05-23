package com.asbresearch.betfair.ref.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Strings;
import java.io.IOException;
import java.time.Instant;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class InstantDeserializer extends JsonDeserializer<Instant> {
    public static final String UTC_CODE = "Z";

    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            String dateAsString = jp.getText();
            if (Strings.isNullOrEmpty(dateAsString)) {
                return null;
            } else {
                return toInstant(dateAsString);
            }
        } catch (IOException pe) {
            throw new RuntimeException(pe);
        }
    }

    public static Instant toInstant(String dateAsString) {
        if (!dateAsString.endsWith(UTC_CODE)) {
            dateAsString = dateAsString + UTC_CODE;
        }
        return Instant.from(ISO_INSTANT.parse(dateAsString));
    }
}
