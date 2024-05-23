package com.asbresearch.betfair.ref.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class InstantSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) {
        try {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(toDateStr(value));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toDateStr(Instant value) {
        return ISO_INSTANT.format(value);
    }
}
